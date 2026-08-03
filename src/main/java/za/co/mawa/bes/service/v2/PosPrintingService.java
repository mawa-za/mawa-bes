package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.*;
import za.co.mawa.bes.dto.v2.ReceiptPrintDto;
import za.co.mawa.bes.entity.v2.PosPrintAgentEntity;
import za.co.mawa.bes.entity.v2.PosPrintAttemptEntity;
import za.co.mawa.bes.entity.v2.PosPrintEnrollmentEntity;
import za.co.mawa.bes.entity.v2.PosPrintJobEntity;
import za.co.mawa.bes.entity.v2.PosPrinterEntity;
import za.co.mawa.bes.entity.v2.PosTerminalEntity;
import za.co.mawa.bes.repository.v2.PosPrintAgentRepository;
import za.co.mawa.bes.repository.v2.PosPrintAttemptRepository;
import za.co.mawa.bes.repository.v2.PosPrintEnrollmentRepository;
import za.co.mawa.bes.repository.v2.PosPrintJobRepository;
import za.co.mawa.bes.repository.v2.PosPrinterRepository;
import za.co.mawa.bes.repository.v2.PosTerminalRepository;
import za.co.mawa.bes.service.CompanyInfoService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PosPrintingService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CLAIM_LEASE_SECONDS = 90;
    private static final int ONLINE_HEARTBEAT_SECONDS = 90;
    private static final int DEFAULT_PAPER_WIDTH = 42;

    private final PosPrintAgentRepository agentRepository;
    private final PosPrinterRepository printerRepository;
    private final PosTerminalRepository terminalRepository;
    private final PosPrintEnrollmentRepository enrollmentRepository;
    private final PosPrintJobRepository jobRepository;
    private final PosPrintAttemptRepository attemptRepository;
    private final ReceiptService receiptService;
    private final CompanyInfoService companyInfoService;

    @Transactional
    public EnrollmentResponse createEnrollment(EnrollmentCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentName())) {
            throw new IllegalArgumentException("Agent name is required");
        }

        String code = uniqueEnrollmentCode();
        int validMinutes = request.getValidMinutes() == null
                ? 30
                : Math.max(5, Math.min(request.getValidMinutes(), 1440));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(validMinutes);

        enrollmentRepository.save(PosPrintEnrollmentEntity.builder()
                .codeHash(hash(code))
                .agentName(request.getAgentName().trim())
                .location(trim(request.getLocation()))
                .expiresAt(expiresAt)
                .createdBy(currentUser())
                .build());

        return EnrollmentResponse.builder()
                .code(code)
                .expiresAt(expiresAt)
                .agentName(request.getAgentName().trim())
                .location(trim(request.getLocation()))
                .build();
    }

    @Transactional
    public AgentEnrollResponse enroll(AgentEnrollRequest request, String remoteIp) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new SecurityException("Enrollment code is required");
        }

        String codeHash = hash(request.getCode().trim().toUpperCase(Locale.ROOT));
        PosPrintEnrollmentEntity enrollment = enrollmentRepository
                .findByCodeHashAndUsedAtIsNull(codeHash)
                .orElseThrow(() -> new SecurityException("Invalid enrollment code"));

        if (enrollment.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new SecurityException("Enrollment code has expired");
        }

        String secret = randomSecret();
        PosPrintAgentEntity agent = agentRepository.save(PosPrintAgentEntity.builder()
                .agentSecretHash(hash(secret))
                .name(enrollment.getAgentName())
                .location(enrollment.getLocation())
                .machineName(trim(request.getMachineName()))
                .osName(trim(request.getOsName()))
                .osVersion(trim(request.getOsVersion()))
                .agentVersion(trim(request.getAgentVersion()))
                .lastIpAddress(remoteIp)
                .lastHeartbeatAt(LocalDateTime.now())
                .build());

        enrollment.setUsedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        return AgentEnrollResponse.builder()
                .agentId(agent.getId())
                .agentSecret(secret)
                .agentName(agent.getName())
                .location(agent.getLocation())
                .build();
    }

    @Transactional
    public void heartbeat(String agentId, String secret, HeartbeatRequest request, String remoteIp) {
        PosPrintAgentEntity agent = validateAgent(agentId, secret);
        if (request != null) {
            agent.setMachineName(trim(request.getMachineName()));
            agent.setOsName(trim(request.getOsName()));
            agent.setOsVersion(trim(request.getOsVersion()));
            agent.setAgentVersion(trim(request.getAgentVersion()));
        }
        agent.setLastIpAddress(remoteIp);
        agent.setLastHeartbeatAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        agentRepository.save(agent);
    }

    @Transactional
    public List<PrinterResponse> syncPrinters(String agentId, String secret, PrinterSyncRequest request) {
        validateAgent(agentId, secret);
        LocalDateTime now = LocalDateTime.now();
        Map<String, PrinterSyncItem> seen = new LinkedHashMap<>();

        if (request != null && request.getPrinters() != null) {
            for (PrinterSyncItem printer : request.getPrinters()) {
                if (printer != null && StringUtils.hasText(printer.getWindowsQueueName())) {
                    seen.put(printer.getWindowsQueueName().trim(), printer);
                }
            }
        }

        for (PosPrinterEntity existing : printerRepository.findByAgentIdOrderByDisplayNameAsc(agentId)) {
            if (!seen.containsKey(existing.getWindowsQueueName())) {
                existing.setStatus("OFFLINE");
                existing.setDefaultPrinter(false);
                existing.setUpdatedAt(now);
                printerRepository.save(existing);
            }
        }

        for (Map.Entry<String, PrinterSyncItem> entry : seen.entrySet()) {
            PrinterSyncItem discovered = entry.getValue();
            Optional<PosPrinterEntity> existingPrinter = printerRepository
                    .findByAgentIdAndWindowsQueueName(agentId, entry.getKey());
            boolean newlyDiscovered = existingPrinter.isEmpty();
            PosPrinterEntity printer = existingPrinter.orElseGet(() -> PosPrinterEntity.builder()
                    .agentId(agentId)
                    .windowsQueueName(entry.getKey())
                    .printerRole("RECEIPT")
                    .build());

            if (newlyDiscovered || !StringUtils.hasText(printer.getDisplayName())) {
                printer.setDisplayName(StringUtils.hasText(discovered.getDisplayName())
                        ? discovered.getDisplayName().trim()
                        : entry.getKey());
            }
            printer.setStatus("ONLINE");
            printer.setDefaultPrinter(Boolean.TRUE.equals(discovered.getDefaultPrinter()));
            // Discovery supplies safe initial defaults. User-configured capabilities are preserved on later syncs.
            if (newlyDiscovered) {
                printer.setSupportsCut(Boolean.TRUE.equals(discovered.getSupportsCut()));
                printer.setPaperWidthChars(discovered.getPaperWidthChars() == null
                        ? DEFAULT_PAPER_WIDTH
                        : Math.max(20, Math.min(discovered.getPaperWidthChars(), 80)));
            }
            printer.setLastSeenAt(now);
            printer.setUpdatedAt(now);
            printerRepository.save(printer);
        }

        return printers(agentId);
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> listAgents() {
        return agentRepository.findAllByOrderByNameAsc().stream().map(this::agentDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PrinterResponse> printers(String agentId) {
        return printerRepository.findByAgentIdOrderByDisplayNameAsc(agentId)
                .stream()
                .map(this::printerDto)
                .toList();
    }

    @Transactional
    public PrinterResponse configurePrinter(String printerId, PrinterConfigurationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Printer configuration is required");
        }
        PosPrinterEntity printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        if (StringUtils.hasText(request.getDisplayName())) {
            printer.setDisplayName(limit(request.getDisplayName().trim(), 255));
        }
        if (StringUtils.hasText(request.getPrinterRole())) {
            String role = request.getPrinterRole().trim().toUpperCase(Locale.ROOT);
            if (!List.of("RECEIPT", "DOCUMENT", "LABEL", "BACKUP_RECEIPT").contains(role)) {
                throw new IllegalArgumentException("Unsupported printer role");
            }
            printer.setPrinterRole(role);
        }
        if (request.getSupportsCut() != null) {
            printer.setSupportsCut(request.getSupportsCut());
        }
        if (request.getPaperWidthChars() != null) {
            if (request.getPaperWidthChars() < 20 || request.getPaperWidthChars() > 80) {
                throw new IllegalArgumentException("Paper width must be between 20 and 80 characters");
            }
            printer.setPaperWidthChars(request.getPaperWidthChars());
        }
        printer.setUpdatedAt(LocalDateTime.now());
        return printerDto(printerRepository.save(printer));
    }

    @Transactional
    public TerminalResponse registerTerminal(TerminalRegisterRequest request) {
        if (request == null || !StringUtils.hasText(request.getTerminalKey())) {
            throw new IllegalArgumentException("Terminal key is required");
        }

        String terminalKey = request.getTerminalKey().trim();
        PosTerminalEntity terminal = terminalRepository.findByTerminalKey(terminalKey)
                .orElseGet(() -> PosTerminalEntity.builder().terminalKey(terminalKey).enabled(true).build());

        terminal.setDisplayName(StringUtils.hasText(request.getDisplayName())
                ? request.getDisplayName().trim()
                : "MAWA ERP Terminal");
        terminal.setLocation(trim(request.getLocation()));
        terminal.setLastSeenAt(LocalDateTime.now());
        terminal.setUpdatedAt(LocalDateTime.now());
        return terminalDto(terminalRepository.save(terminal));
    }

    @Transactional(readOnly = true)
    public List<TerminalResponse> listTerminals() {
        return terminalRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(this::terminalDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TerminalResponse getTerminalByKey(String terminalKey) {
        return terminalDto(terminalRepository.findByTerminalKey(terminalKey)
                .orElseThrow(() -> new IllegalArgumentException("Terminal is not registered")));
    }

    @Transactional
    public TerminalResponse assignTerminal(String terminalId, TerminalAssignmentRequest request) {
        PosTerminalEntity terminal = terminalRepository.findById(terminalId)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found"));
        if (request == null || !StringUtils.hasText(request.getAgentId())) {
            throw new IllegalArgumentException("Agent is required");
        }

        PosPrintAgentEntity agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        if (!"ACTIVE".equals(agent.getStatus())) {
            throw new IllegalArgumentException("Agent is not active");
        }

        if (StringUtils.hasText(request.getDefaultReceiptPrinterId())) {
            validatePrinterAssignment(request.getDefaultReceiptPrinterId(), agent.getId());
        }
        if (StringUtils.hasText(request.getDefaultDocumentPrinterId())) {
            validatePrinterAssignment(request.getDefaultDocumentPrinterId(), agent.getId());
        }

        terminal.setAgentId(agent.getId());
        terminal.setDefaultReceiptPrinterId(trim(request.getDefaultReceiptPrinterId()));
        terminal.setDefaultDocumentPrinterId(trim(request.getDefaultDocumentPrinterId()));
        terminal.setUpdatedAt(LocalDateTime.now());
        return terminalDto(terminalRepository.save(terminal));
    }

    @Transactional
    public TerminalResponse setTerminalEnabled(String terminalId, TerminalEnabledRequest request) {
        if (request == null || request.getEnabled() == null) {
            throw new IllegalArgumentException("Enabled status is required");
        }
        PosTerminalEntity terminal = terminalRepository.findById(terminalId)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found"));
        terminal.setEnabled(request.getEnabled());
        terminal.setUpdatedAt(LocalDateTime.now());
        return terminalDto(terminalRepository.save(terminal));
    }

    @Transactional
    public void revokeAgent(String agentId) {
        PosPrintAgentEntity agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        agent.setStatus("REVOKED");
        agent.setUpdatedAt(LocalDateTime.now());
        agentRepository.save(agent);
        jobRepository.failOpenForAgent(agentId, LocalDateTime.now(), "Print agent was revoked");

        for (PosPrinterEntity printer : printerRepository.findByAgentIdOrderByDisplayNameAsc(agentId)) {
            printer.setStatus("OFFLINE");
            printer.setUpdatedAt(LocalDateTime.now());
            printerRepository.save(printer);
        }
    }

    @Transactional
    public PrintJobResponse queueReceipt(String receiptId, QueueReceiptRequest request) {
        ReceiptPrintDto data = receiptService.previewPrintData(receiptId);
        boolean reprint = Boolean.TRUE.equals(request == null ? null : request.getReprint())
                || (data.getPrintCount() != null && data.getPrintCount() > 0);
        if (request != null && !reprint) {
            // The first print is deterministic, so double-clicks and HTTP retries cannot create duplicate jobs.
            request.setRequestId("INITIAL");
        }
        PosPrinterEntity selectedPrinter = selectedPrinter(request);
        String content = renderReceipt(data, reprint, paperWidth(selectedPrinter));
        return queueContent("RECEIPT", receiptId, receiptId, content, request);
    }

    @Transactional
    public PrintJobResponse queueTestPrint(String terminalId, QueueReceiptRequest request) {
        QueueReceiptRequest destination = request == null ? new QueueReceiptRequest() : request;
        destination.setTerminalId(terminalId);
        if (!StringUtils.hasText(destination.getRequestId())) {
            destination.setRequestId(UUID.randomUUID().toString());
        }

        PosPrinterEntity selectedPrinter = selectedPrinter(destination);
        int width = paperWidth(selectedPrinter);
        String separator = "-".repeat(width);
        String content = center("MAWA POS TEST PRINT", width) + "\n"
                + separator + "\n"
                + "Terminal and printer setup is working.\n"
                + "Queue: " + selectedPrinter.getWindowsQueueName() + "\n"
                + "Width: " + width + " characters\n"
                + "Cutter: " + (selectedPrinter.isSupportsCut() ? "enabled" : "disabled") + "\n"
                + "Time: " + LocalDateTime.now() + "\n"
                + separator + "\n";
        return queueContent("TEST_PRINT", terminalId, null, content, destination);
    }

    @Transactional
    public PrintJobResponse queueContent(
            String sourceType,
            String sourceId,
            String receiptId,
            String content,
            QueueReceiptRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Print destination is required");
        }
        if (!StringUtils.hasText(sourceType) || !StringUtils.hasText(sourceId)) {
            throw new IllegalArgumentException("Print job source is required");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Print content is empty");
        }

        PosTerminalEntity terminal = resolveTerminal(request);
        if (!terminal.isEnabled()) {
            throw new IllegalArgumentException("Terminal is disabled");
        }
        if (!StringUtils.hasText(terminal.getAgentId())) {
            throw new IllegalArgumentException("Terminal has no assigned print agent");
        }

        PosPrintAgentEntity agent = agentRepository.findById(terminal.getAgentId())
                .orElseThrow(() -> new IllegalArgumentException("Assigned print agent no longer exists"));
        if (!"ACTIVE".equals(agent.getStatus())) {
            throw new IllegalArgumentException("Assigned print agent is not active");
        }

        String printerId = StringUtils.hasText(request.getPrinterId())
                ? request.getPrinterId().trim()
                : terminal.getDefaultReceiptPrinterId();
        if (!StringUtils.hasText(printerId)) {
            throw new IllegalArgumentException("Terminal has no assigned receipt printer");
        }

        PosPrinterEntity printer = validatePrinterAssignment(printerId, terminal.getAgentId());
        if (!"ONLINE".equals(printer.getStatus())) {
            throw new IllegalArgumentException("Selected printer is offline");
        }

        String requestId = StringUtils.hasText(request.getRequestId())
                ? request.getRequestId().trim()
                : UUID.randomUUID().toString();
        String idempotencyKey = hash(sourceType + "|" + sourceId + "|" + terminal.getId() + "|" + requestId);
        Optional<PosPrintJobEntity> existing = jobRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            PosPrintJobEntity existingJob = existing.get();
            if ("FAILED".equals(existingJob.getStatus())) {
                return retry(existingJob.getId());
            }
            return jobDto(existingJob, printer);
        }

        PosPrintJobEntity job = PosPrintJobEntity.builder()
                .receiptId(receiptId)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .terminalId(terminal.getId())
                .agentId(terminal.getAgentId())
                .printerId(printerId)
                .content(content)
                .contentType("ESC_POS_TEXT")
                .status("QUEUED")
                .priority(0)
                .idempotencyKey(idempotencyKey)
                .maxAttempts(5)
                .createdBy(currentUser())
                .build();
        return jobDto(jobRepository.save(job), printer);
    }

    @Transactional
    public PrintJobResponse claim(String agentId, String secret) {
        validateAgent(agentId, secret);
        LocalDateTime now = LocalDateTime.now();
        jobRepository.failExhaustedExpired(agentId, now);
        jobRepository.releaseExpired(agentId, now);

        List<PosPrintJobEntity> jobs = jobRepository.findClaimable(agentId, now, PageRequest.of(0, 1));
        if (jobs.isEmpty()) {
            return null;
        }

        PosPrintJobEntity job = jobs.get(0);
        job.setStatus("CLAIMED");
        job.setClaimToken(UUID.randomUUID().toString());
        job.setClaimedByAgentId(agentId);
        job.setClaimedAt(now);
        job.setClaimExpiresAt(now.plusSeconds(CLAIM_LEASE_SECONDS));
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setUpdatedAt(now);
        jobRepository.save(job);

        attemptRepository.save(PosPrintAttemptEntity.builder()
                .printJobId(job.getId())
                .agentId(agentId)
                .printerId(job.getPrinterId())
                .attemptNumber(job.getAttemptCount())
                .status("CLAIMED")
                .build());

        return jobDto(job, printerRepository.findById(job.getPrinterId()).orElse(null));
    }

    @Transactional
    public void markSpooled(String agentId, String secret, String jobId, JobResultRequest request) {
        validateAgent(agentId, secret);
        PosPrintJobEntity existing = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Print job not found"));
        if ("SPOOLED".equals(existing.getStatus())
                && agentId.equals(existing.getAgentId())
                && request != null
                && constantEquals(existing.getClaimToken(), request.getClaimToken())) {
            return;
        }
        PosPrintJobEntity job = validateClaim(agentId, jobId, request);
        LocalDateTime now = LocalDateTime.now();

        job.setStatus("SPOOLED");
        job.setSpooledAt(now);
        job.setClaimExpiresAt(null);
        job.setUpdatedAt(now);
        jobRepository.save(job);

        attemptRepository.findTopByPrintJobIdOrderByAttemptNumberDesc(jobId).ifPresent(attempt -> {
            attempt.setStatus("SPOOLED");
            attempt.setCompletedAt(now);
            attemptRepository.save(attempt);
        });

        if (StringUtils.hasText(job.getReceiptId())) {
            receiptService.recordSpooledPrint(job.getReceiptId());
        }
    }

    @Transactional
    public void markFailed(String agentId, String secret, String jobId, JobResultRequest request) {
        validateAgent(agentId, secret);
        PosPrintJobEntity job = validateClaim(agentId, jobId, request);
        LocalDateTime now = LocalDateTime.now();
        String error = request == null || !StringUtils.hasText(request.getErrorMessage())
                ? "Unknown printing error"
                : limit(request.getErrorMessage().trim(), 2000);

        job.setLastError(error);
        job.setFailedAt(now);
        clearClaim(job);
        if (job.getAttemptCount() < job.getMaxAttempts()) {
            job.setStatus("QUEUED");
            job.setNextAttemptAt(now.plusSeconds(Math.min(60, 1L << Math.min(job.getAttemptCount(), 5))));
        } else {
            job.setStatus("FAILED");
        }
        job.setUpdatedAt(now);
        jobRepository.save(job);

        attemptRepository.findTopByPrintJobIdOrderByAttemptNumberDesc(jobId).ifPresent(attempt -> {
            attempt.setStatus("FAILED");
            attempt.setErrorMessage(error);
            attempt.setCompletedAt(now);
            attemptRepository.save(attempt);
        });
    }

    @Transactional(readOnly = true)
    public List<PrintJobResponse> listJobs() {
        return jobRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(job -> jobDto(job, printerRepository.findById(job.getPrinterId()).orElse(null)))
                .toList();
    }

    @Transactional
    public PrintJobResponse retry(String jobId) {
        PosPrintJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if ("SPOOLED".equals(job.getStatus())) {
            throw new IllegalArgumentException("Spooled jobs cannot be retried; create a reprint instead");
        }

        job.setStatus("QUEUED");
        job.setAttemptCount(0);
        job.setNextAttemptAt(LocalDateTime.now());
        job.setFailedAt(null);
        job.setLastError(null);
        clearClaim(job);
        job.setUpdatedAt(LocalDateTime.now());
        return jobDto(jobRepository.save(job), printerRepository.findById(job.getPrinterId()).orElse(null));
    }

    private PosPrintAgentEntity validateAgent(String agentId, String secret) {
        if (!StringUtils.hasText(agentId) || !StringUtils.hasText(secret)) {
            throw new SecurityException("Agent credentials are required");
        }

        PosPrintAgentEntity agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new SecurityException("Unknown print agent"));
        if (!"ACTIVE".equals(agent.getStatus())
                || !constantEquals(agent.getAgentSecretHash(), hash(secret))) {
            throw new SecurityException("Invalid print agent credentials");
        }
        return agent;
    }

    private PosPrintJobEntity validateClaim(String agentId, String jobId, JobResultRequest request) {
        PosPrintJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Print job not found"));
        if (!agentId.equals(job.getAgentId())
                || request == null
                || !constantEquals(job.getClaimToken(), request.getClaimToken())) {
            throw new SecurityException("Invalid print job claim");
        }
        if (!"CLAIMED".equals(job.getStatus())) {
            throw new IllegalStateException("Print job is not claimed");
        }
        return job;
    }

    private PosTerminalEntity resolveTerminal(QueueReceiptRequest request) {
        if (StringUtils.hasText(request.getTerminalId())) {
            return terminalRepository.findById(request.getTerminalId())
                    .orElseThrow(() -> new IllegalArgumentException("Terminal not found"));
        }
        if (StringUtils.hasText(request.getTerminalKey())) {
            return terminalRepository.findByTerminalKey(request.getTerminalKey())
                    .orElseThrow(() -> new IllegalArgumentException("Terminal not registered"));
        }
        throw new IllegalArgumentException("Terminal ID or terminal key is required");
    }

    private PosPrinterEntity selectedPrinter(QueueReceiptRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Print destination is required");
        }
        PosTerminalEntity terminal = resolveTerminal(request);
        if (!StringUtils.hasText(terminal.getAgentId())) {
            throw new IllegalArgumentException("Terminal has no assigned print agent");
        }
        String printerId = StringUtils.hasText(request.getPrinterId())
                ? request.getPrinterId().trim()
                : terminal.getDefaultReceiptPrinterId();
        if (!StringUtils.hasText(printerId)) {
            throw new IllegalArgumentException("Terminal has no assigned receipt printer");
        }
        return validatePrinterAssignment(printerId, terminal.getAgentId());
    }

    private int paperWidth(PosPrinterEntity printer) {
        return printer == null || printer.getPaperWidthChars() == null
                ? DEFAULT_PAPER_WIDTH
                : Math.max(20, Math.min(printer.getPaperWidthChars(), 80));
    }

    private PosPrinterEntity validatePrinterAssignment(String printerId, String agentId) {
        PosPrinterEntity printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        if (!agentId.equals(printer.getAgentId())) {
            throw new IllegalArgumentException("Printer does not belong to selected agent");
        }
        return printer;
    }

    private AgentResponse agentDto(PosPrintAgentEntity agent) {
        boolean online = "ACTIVE".equals(agent.getStatus())
                && agent.getLastHeartbeatAt() != null
                && agent.getLastHeartbeatAt().isAfter(LocalDateTime.now().minusSeconds(ONLINE_HEARTBEAT_SECONDS));
        return AgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .machineName(agent.getMachineName())
                .location(agent.getLocation())
                .status(agent.getStatus())
                .online(online)
                .agentVersion(agent.getAgentVersion())
                .lastHeartbeatAt(agent.getLastHeartbeatAt())
                .printers(printers(agent.getId()))
                .build();
    }

    private PrinterResponse printerDto(PosPrinterEntity printer) {
        return PrinterResponse.builder()
                .id(printer.getId())
                .agentId(printer.getAgentId())
                .windowsQueueName(printer.getWindowsQueueName())
                .displayName(printer.getDisplayName())
                .printerRole(printer.getPrinterRole())
                .status(printer.getStatus())
                .defaultPrinter(printer.isDefaultPrinter())
                .supportsCut(printer.isSupportsCut())
                .paperWidthChars(printer.getPaperWidthChars())
                .lastSeenAt(printer.getLastSeenAt())
                .build();
    }

    private TerminalResponse terminalDto(PosTerminalEntity terminal) {
        return TerminalResponse.builder()
                .id(terminal.getId())
                .terminalKey(terminal.getTerminalKey())
                .displayName(terminal.getDisplayName())
                .location(terminal.getLocation())
                .agentId(terminal.getAgentId())
                .defaultReceiptPrinterId(terminal.getDefaultReceiptPrinterId())
                .defaultDocumentPrinterId(terminal.getDefaultDocumentPrinterId())
                .enabled(terminal.isEnabled())
                .build();
    }

    private PrintJobResponse jobDto(PosPrintJobEntity job, PosPrinterEntity printer) {
        return PrintJobResponse.builder()
                .id(job.getId())
                .sourceType(job.getSourceType())
                .sourceId(job.getSourceId())
                .receiptId(job.getReceiptId())
                .terminalId(job.getTerminalId())
                .agentId(job.getAgentId())
                .printerId(job.getPrinterId())
                .printerQueueName(printer == null ? null : printer.getWindowsQueueName())
                .printerSupportsCut(printer != null && printer.isSupportsCut())
                .paperWidthChars(printer == null ? DEFAULT_PAPER_WIDTH : printer.getPaperWidthChars())
                .content(job.getContent())
                .contentType(job.getContentType())
                .status(job.getStatus())
                .claimToken(job.getClaimToken())
                .attemptCount(job.getAttemptCount())
                .claimExpiresAt(job.getClaimExpiresAt())
                .createdAt(job.getCreatedAt())
                .lastError(job.getLastError())
                .build();
    }

    private String renderReceipt(ReceiptPrintDto data, boolean reprint, int width) {
        StringBuilder receipt = new StringBuilder();
        if (reprint) {
            receipt.append(center("*** REPRINT ***", width)).append('\n');
        }

        String companyName = nullSafe(companyInfoService.getCompanyName());
        receipt.append(center(companyName.isBlank() ? "MawaPay" : companyName, width)).append('\n');
        appendCenteredDetail(receipt, "Reg: ", companyInfoService.getCompanyRegistrationNumber(), width);
        appendCenteredDetail(receipt, "VAT: ", companyInfoService.getVATNumber(), width);
        appendCenteredDetail(receipt, "FSP: ", companyInfoService.getFspNumber(), width);
        appendCenteredDetail(receipt, "", companyInfoService.getCompanyAddress(), width);
        appendCenteredDetail(receipt, "", companyInfoService.getContactDetails(), width);
        receipt.append('\n')
                .append(center("OFFICIAL RECEIPT", width)).append('\n')
                .append("-".repeat(width)).append('\n');

        line(receipt, "Receipt No", data.getReceiptNo(), width);
        line(receipt, "Trace ID", data.getTraceId(), width);
        line(receipt, "Date", data.getReceiptDate() == null
                ? ""
                : data.getReceiptDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), width);
        receipt.append('\n');
        line(receipt, "Member", data.getMemberName(), width);
        line(receipt, "Membership No", data.getMembershipNo(), width);
        line(receipt, "ID Number", data.getIdentityNumber(), width);
        line(receipt, "Plan", data.getPlanName(), width);
        line(receipt, "Period", data.getPremiumPeriodYYYYMM(), width);
        line(receipt, "Payment", data.getPaymentMethod(), width);
        receipt.append('\n');
        line(receipt, "Amount", money(data.getAmountCents()), width);
        receipt.append('\n');
        line(receipt, "Cashier", data.getEmployeeResponsible(), width);
        receipt.append('\n')
                .append(center("Thank you", width)).append('\n')
                .append('\n')
                .append(center("MawaPay", width)).append('\n');
        return receipt.toString();
    }

    private void appendCenteredDetail(
            StringBuilder receipt,
            String prefix,
            String value,
            int width
    ) {
        String safe = nullSafe(value).trim();
        if (!safe.isEmpty()) {
            receipt.append(center(prefix + safe, width)).append('\n');
        }
    }

    private void line(StringBuilder receipt, String label, String value, int width) {
        String prefix = label + ": ";
        String safeValue = nullSafe(value).replace('\r', ' ').replace('\n', ' ').trim();
        int firstLineCapacity = Math.max(1, width - prefix.length());
        if (safeValue.length() <= firstLineCapacity) {
            receipt.append(prefix).append(safeValue).append('\n');
            return;
        }

        receipt.append(prefix).append(safeValue, 0, firstLineCapacity).append('\n');
        int offset = firstLineCapacity;
        String indent = " ".repeat(Math.min(prefix.length(), Math.max(0, width - 1)));
        int continuationCapacity = Math.max(1, width - indent.length());
        while (offset < safeValue.length()) {
            int end = Math.min(safeValue.length(), offset + continuationCapacity);
            receipt.append(indent).append(safeValue, offset, end).append('\n');
            offset = end;
        }
    }

    private String money(Long cents) {
        return cents == null ? "" : "R " + BigDecimal.valueOf(cents, 2).toPlainString();
    }

    private String center(String value, int width) {
        String safe = limit(nullSafe(value), width);
        int padding = Math.max(0, (width - safe.length()) / 2);
        return " ".repeat(padding) + safe;
    }

    private void clearClaim(PosPrintJobEntity job) {
        job.setClaimToken(null);
        job.setClaimedByAgentId(null);
        job.setClaimedAt(null);
        job.setClaimExpiresAt(null);
    }

    private String currentUser() {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                return "system";
            }
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {
            return "system";
        }
    }

    private String uniqueEnrollmentCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = randomCode();
            if (enrollmentRepository.findByCodeHashAndUsedAtIsNull(hash(code)).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique enrollment code");
    }

    private String randomCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return code.toString();
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash value", exception);
        }
    }

    private boolean constantEquals(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8));
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
