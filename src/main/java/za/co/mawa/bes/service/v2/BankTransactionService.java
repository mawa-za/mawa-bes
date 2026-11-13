package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.repository.v2.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class BankTransactionService {
    private final BankStatementCsvParser parser;
    private final BankStatementImportRepository imports;
    private final BankTransactionRepository transactions;
    private final BankTransactionAllocationRepository allocations;

    public record BankDeposit(String depositId, LocalDate date, long amountCents, String reference,
                              String accountNumber, Long balanceCents, long allocatedCents,
                              long availableCents, String status) { }

    public static String formatId(Long id) { return "DEP-" + String.format(Locale.ROOT, "%08d", id); }

    public static Long parseId(String depositId) {
        if (depositId == null || !depositId.trim().toUpperCase(Locale.ROOT).matches("DEP-[0-9]{1,18}")) {
            throw new IllegalArgumentException("A valid bank Deposit ID (DEP-00000123) is required");
        }
        return Long.parseLong(depositId.trim().substring(4));
    }

    @Transactional(readOnly = true)
    public Page<BankDeposit> search(String search, boolean creditsOnly, LocalDate fromDate,
                                    LocalDate toDate, Long amountCents, Pageable pageable) {
        String normalized = search == null ? "" : search.trim();
        if (normalized.toUpperCase(Locale.ROOT).startsWith("DEP-")) {
            normalized = String.valueOf(parseId(normalized));
        }
        return transactions.search(normalized, creditsOnly, fromDate, toDate, amountCents, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public BankDeposit get(String depositId) {
        return toDto(transactions.findById(parseId(depositId))
                .orElseThrow(() -> new IllegalArgumentException("Bank deposit was not found")));
    }

    private BankDeposit toDto(BankTransactionEntity t) {
        long allocated = allocations.usedAmount(t.getId());
        long available = Math.max(0, t.getAmountCents() - allocated);
        return new BankDeposit(formatId(t.getId()), t.getTransactionDate(), t.getAmountCents(),
                t.getReferenceText(), t.getAccountNumber(), t.getBalanceCents(), allocated, available,
                t.getAmountCents() <= 0 ? "DEBIT" : available == 0 ? "ALLOCATED" : allocated > 0 ? "PARTIAL" : "AVAILABLE");
    }

    /** Invoked after the business object has been created inside the same DB transaction. */
    @Transactional
    public Long allocate(String depositId, long amountCents, String type, String objectId, String actor) {
        if (amountCents <= 0) throw new IllegalArgumentException("Deposit allocation amount must be positive");
        BankTransactionEntity transaction = transactions.findLocked(parseId(depositId))
                .orElseThrow(() -> new IllegalArgumentException("Deposit ID not found in bank history"));
        if (transaction.getAmountCents() <= 0) {
            throw new IllegalArgumentException("A debit cannot be used to verify incoming payments");
        }
        long allocated = allocations.usedAmount(transaction.getId());
        if (allocated > transaction.getAmountCents() - amountCents) {
            throw new IllegalArgumentException("Deposit ID has insufficient unallocated funds. Available: "
                    + (transaction.getAmountCents() - allocated) + " cents");
        }
        BankTransactionAllocationEntity entry = new BankTransactionAllocationEntity();
        entry.setBankTransactionId(transaction.getId());
        entry.setAllocationType(type);
        entry.setObjectId(objectId);
        entry.setAmountCents(amountCents);
        entry.setCreatedBy(actor);
        allocations.saveAndFlush(entry);
        return transaction.getId();
    }

    @Transactional
    public void release(String type, String objectId, String actor) {
        allocations.findByAllocationTypeAndObjectId(type, objectId).ifPresent(entry -> {
            if (entry.getReleasedAt() == null) {
                entry.setReleasedAt(java.time.LocalDateTime.now());
                entry.setReleasedBy(actor);
                allocations.save(entry);
            }
        });
    }

    public record Allocation(String type, String objectId, long amountCents, String createdBy,
                             java.time.LocalDateTime createdAt, java.time.LocalDateTime releasedAt) { }

    @Transactional(readOnly = true)
    public List<Allocation> allocations(String depositId) {
        Long id = parseId(depositId);
        if (!transactions.existsById(id)) throw new IllegalArgumentException("Bank deposit was not found");
        return allocations.findByBankTransactionIdOrderByCreatedAtDesc(id).stream()
                .map(a -> new Allocation(a.getAllocationType(), a.getObjectId(), a.getAmountCents(),
                        a.getCreatedBy(), a.getCreatedAt(), a.getReleasedAt())).toList();
    }

    /** Backward-compatible entry point for any existing callers. */
    @Transactional
    public int importCsv(String provider, String messageId, String attachmentId, String fileName, byte[] bytes,
                         String allowedAccounts) {
        return importCsv(provider, messageId, attachmentId, fileName, bytes, allowedAccounts, null);
    }

    /** A failed CSV import rolls back its envelope and all its rows. */
    @Transactional
    public int importCsv(String provider, String messageId, String attachmentId, String fileName, byte[] bytes,
                         String allowedAccounts, Long sourceFileId) {
        if (imports.existsByProviderAndMessageIdAndAttachmentId(provider, messageId, attachmentId)) return 0;
        List<BankStatementCsvParser.Transaction> rows = parser.parse(bytes);
        if (allowedAccounts == null || allowedAccounts.isBlank())
            throw new IllegalArgumentException("Allowed bank accounts are not configured");
        java.util.Set<String> whitelist = new java.util.HashSet<>(java.util.Arrays.asList(allowedAccounts.split(",")));
        if (rows.stream().anyMatch(row -> !whitelist.contains(row.accountNumber()))) {
            throw new IllegalArgumentException("Statement account is not permitted by tenant configuration");
        }
        BankStatementImportEntity envelope = new BankStatementImportEntity();
        envelope.setProvider(provider);
        envelope.setMessageId(messageId);
        envelope.setAttachmentId(attachmentId);
        envelope.setFileName(fileName);
        envelope.setSha256(BankStatementCsvParser.sha256(bytes));
        envelope.setSourceFileId(sourceFileId);
        envelope = imports.saveAndFlush(envelope);
        int count = 0;
        for (var row : rows) {
            if (transactions.existsByFingerprint(row.fingerprint())) continue;
            BankTransactionEntity t = new BankTransactionEntity();
            t.setSourceImportId(envelope.getId());
            t.setFingerprint(row.fingerprint());
            t.setAccountNumber(row.accountNumber());
            t.setTransactionDate(row.date());
            t.setAmountCents(row.amountCents());
            t.setServiceFeeCents(row.feeCents());
            t.setReferenceText(row.reference());
            t.setChequeNumber(row.chequeNumber());
            t.setBalanceCents(row.balanceCents());
            transactions.save(t);
            count++;
        }
        envelope.setImportedCount(count);
        imports.save(envelope);
        transactions.flush();
        return count;
    }
}
