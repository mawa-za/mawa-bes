package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.BankStatementFileEntity;
import za.co.mawa.bes.entity.v2.BankStatementFileAttemptEntity;
import za.co.mawa.bes.repository.v2.BankStatementFileRepository;
import za.co.mawa.bes.repository.v2.BankStatementFileAttemptRepository;
import za.co.mawa.bes.service.AttachmentStorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** All import sources persist the original attachment BEFORE extracting any financial data. */
@Service @RequiredArgsConstructor
public class BankStatementFileService {
    private static final int MAX_BYTES = 5_000_000;
    private final BankStatementFileRepository files;
    private final BankStatementFileAttemptRepository attempts;
    private final AttachmentStorageService storage;
    private final BankTransactionService transactions;
    private final BankStatementMailConfigurationService configuration;

    public record FileView(Long id, String source, String provider, String fileName, String sha256,
                           long byteSize, String status, int importedCount, int csvCount,
                           int failedCsvCount, int attemptCount, String lastError, String uploadedBy,
                           LocalDateTime createdAt, LocalDateTime lastProcessedAt, boolean stored) { }
    public record AttemptView(Long id, int attemptNumber, String attemptedBy, String status,
                              int importedCount, int csvCount, int failedCsvCount,
                              String summary, LocalDateTime startedAt, LocalDateTime finishedAt) { }
    public record ReceiveResult(FileView file, boolean alreadyReceived) { }

    public Page<FileView> search(String source, String status, String search, Pageable pageable) {
        return files.search(source == null ? "" : source.trim(), status == null ? "" : status.trim(),
                search == null ? "" : search.trim(), pageable).map(this::view);
    }

    public FileView get(Long id) { return view(find(id)); }

    public byte[] originalFile(Long id) {
        BankStatementFileEntity file = find(id);
        if (file.getStoragePath() == null) throw new IllegalStateException("Source file has not been archived");
        byte[] bytes = storage.read(file.getStorageBucket(), file.getStoragePath());
        if (bytes.length != file.getByteSize()
                || !BankStatementCsvParser.sha256(bytes).equals(file.getSha256()))
            throw new IllegalStateException("Archived bank statement integrity check failed");
        return bytes;
    }

    public List<AttemptView> history(Long id) {
        find(id);
        return attempts.findByFileIdOrderByStartedAtDescIdDesc(id).stream()
                .map(a -> new AttemptView(a.getId(), a.getAttemptNumber(), a.getAttemptedBy(),
                        a.getStatus(), a.getImportedCount(), a.getCsvCount(), a.getFailedCsvCount(),
                        a.getSummary(), a.getStartedAt(), a.getFinishedAt())).toList();
    }

    public ReceiveResult manualUpload(String fileName, byte[] content, String actor) {
        return receive("MANUAL", "MANUAL", null, null, fileName, content, actor);
    }

    public ReceiveResult emailAttachment(String provider, String messageId, String attachmentId,
                                         String fileName, byte[] content) {
        return receive("EMAIL", provider, messageId, attachmentId, fileName, content, "SYSTEM:MAIL");
    }

    private ReceiveResult receive(String source, String provider, String messageId, String attachmentId,
                                  String fileName, byte[] content, String actor) {
        if (!BankStatementAttachmentExtractor.supported(fileName))
            throw new IllegalArgumentException("Only FNB statement CSV or ZIP files are supported");
        if (content == null || content.length == 0 || content.length > MAX_BYTES)
            throw new IllegalArgumentException("Statement file must be between 1 byte and 5 MB");
        if ("EMAIL".equals(source)) {
            var previous = files.findBySourceAndProviderAndMessageIdAndAttachmentId(
                    source, provider, messageId, attachmentId);
            if (previous.isPresent()) {
                var existing = previous.get();
                if (existing.getStoragePath() == null) {
                    if (!existing.getSha256().equals(BankStatementCsvParser.sha256(content)))
                        throw new IllegalStateException("Mailbox attachment changed after initial receipt");
                    // A process crash or GCS outage must not permanently suppress this email.
                    archive(existing, content);
                    return new ReceiveResult(view(existing), false);
                }
                return new ReceiveResult(view(existing), true);
            }
        }
        String safeName = fileName.replace('\\', '/');
        safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
        if (safeName.isBlank() || safeName.length() > 255)
            throw new IllegalArgumentException("Invalid bank statement filename");
        BankStatementFileEntity file = new BankStatementFileEntity();
        file.setSource(source); file.setProvider(provider);
        file.setMessageId(messageId); file.setAttachmentId(attachmentId);
        file.setFileName(safeName); file.setSha256(BankStatementCsvParser.sha256(content));
        file.setByteSize((long) content.length);
        file.setUploadedBy(actor == null || actor.isBlank() ? "SYSTEM" : actor);
        try {
            file = files.saveAndFlush(file);
        } catch (DataIntegrityViolationException duplicate) {
            if ("EMAIL".equals(source)) {
                return files.findBySourceAndProviderAndMessageIdAndAttachmentId(
                        source, provider, messageId, attachmentId)
                        .map(existing -> new ReceiveResult(view(existing), true))
                        .orElseThrow(() -> duplicate);
            }
            throw duplicate;
        }
        archive(file, content);
        return new ReceiveResult(view(file), false);
    }

    private void archive(BankStatementFileEntity file, byte[] content) {
        try {
            String extension = file.getFileName().toLowerCase(Locale.ROOT).endsWith(".zip") ? "zip" : "csv";
            var stored = storage.store(content, extension, "bank-statements", file.getId().toString(),
                    "original", file.getFileName());
            file.setStorageBucket(stored.bucket());
            file.setStoragePath(stored.path());
            file.setStatus("STORED");
            file.setLastError(null);
        } catch (RuntimeException ex) {
            file.setStatus("STORAGE_FAILED");
            file.setLastError("Could not preserve source file in GCS (" + ex.getClass().getSimpleName() + ")");
            files.saveAndFlush(file);
            throw new IllegalStateException("Bank statement upload could not be archived in GCS; no transactions were imported");
        }
        files.saveAndFlush(file);
    }

    /** Replays only the GCS source, never data provided by an untrusted retry request. */
    public FileView process(Long id, String actor) {
        LocalDateTime now = LocalDateTime.now();
        if (files.claim(id, now, now.minusMinutes(30)) != 1)
            throw new IllegalStateException("File is completed, unavailable or already being processed");
        BankStatementFileEntity file = find(id);
        for (var interrupted : attempts.findByFileIdOrderByStartedAtDescIdDesc(id)) {
            if ("PROCESSING".equals(interrupted.getStatus())) {
                interrupted.setStatus("INTERRUPTED");
                interrupted.setFinishedAt(now);
                interrupted.setSummary("Previous processing was interrupted; a new attempt has started");
                attempts.saveAndFlush(interrupted);
            }
        }
        BankStatementFileAttemptEntity attempt = new BankStatementFileAttemptEntity();
        attempt.setFileId(file.getId());
        attempt.setAttemptNumber(file.getAttemptCount());
        attempt.setAttemptedBy(actor == null || actor.isBlank() ? "SYSTEM" : actor);
        attempt = attempts.saveAndFlush(attempt);
        int imported = 0, csvCount = 0, failedCsvCount = 0;
        String summary = null;
        try {
            byte[] bytes = originalFile(id);
            List<BankStatementAttachmentExtractor.CsvFile> csvFiles =
                    BankStatementAttachmentExtractor.extract(file.getFileName(), bytes);
            if (csvFiles.isEmpty()) throw new IllegalArgumentException("No CSV files found in bank statement ZIP");
            String allowedAccounts = configuration.get().accountNumbers();
            for (var csv : csvFiles) {
                csvCount++;
                try {
                    imported += transactions.importCsv(file.getProvider(),
                            file.getMessageId() == null ? "manual-file-" + file.getId() : file.getMessageId(),
                            BankStatementAttachmentExtractor.attachmentIdentity(
                                    file.getAttachmentId() == null ? "manual-file-" + file.getId() : file.getAttachmentId(), csv),
                            csv.fileName(), csv.bytes(), allowedAccounts, file.getId());
                } catch (RuntimeException ex) {
                    failedCsvCount++;
                    // Do not expose bank statement content, account numbers or raw exception text.
                    summary = issue(ex, "CSV processing failed");
                }
            }
            file.setStatus(failedCsvCount == 0 ? "COMPLETED" : failedCsvCount == csvCount ? "FAILED" : "PARTIAL");
        } catch (RuntimeException ex) {
            file.setStatus("FAILED");
            summary = issue(ex, "File processing failed");
        }
        file.setImportedCount(file.getImportedCount() + imported);
        file.setCsvCount(csvCount);
        file.setFailedCsvCount(failedCsvCount);
        file.setLastError(summary);
        file.setLastProcessedAt(LocalDateTime.now());
        attempt.setStatus(file.getStatus());
        attempt.setImportedCount(imported);
        attempt.setCsvCount(csvCount);
        attempt.setFailedCsvCount(failedCsvCount);
        attempt.setSummary(summary);
        attempt.setFinishedAt(LocalDateTime.now());
        attempts.saveAndFlush(attempt);
        return view(files.saveAndFlush(file));
    }

    /** Human-readable categories without leaking account values or source document contents. */
    private static String issue(RuntimeException ex, String fallback) {
        String message = ex.getMessage();
        if ("Allowed bank accounts are not configured".equals(message))
            return "Configure allowed bank account numbers before processing this statement";
        if ("Statement account is not permitted by tenant configuration".equals(message))
            return "Statement contains a bank account that is not permitted for this tenant";
        if (message != null && message.startsWith("Unsupported bank statement CSV"))
            return "Unsupported CSV format (FNB account transaction history expected)";
        if (message != null && message.startsWith("No CSV files found"))
            return "ZIP archive does not contain a supported CSV file";
        if (message != null && message.startsWith("Archived bank statement integrity"))
            return "Archived file failed integrity validation; contact support";
        return fallback + " (" + ex.getClass().getSimpleName() + ")";
    }

    private BankStatementFileEntity find(Long id) {
        return files.findById(id).orElseThrow(() -> new IllegalArgumentException("Statement file not found"));
    }
    private FileView view(BankStatementFileEntity f) {
        return new FileView(f.getId(), f.getSource(), f.getProvider(), f.getFileName(), f.getSha256(),
                f.getByteSize(), f.getStatus(), f.getImportedCount(), f.getCsvCount(),
                f.getFailedCsvCount(), f.getAttemptCount(), f.getLastError(), f.getUploadedBy(),
                f.getCreatedAt(), f.getLastProcessedAt(), f.getStoragePath() != null);
    }
}
