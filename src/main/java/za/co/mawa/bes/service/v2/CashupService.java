package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.payapp.*;
import com.google.gson.Gson;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.entity.v2.CashupDepositEntity;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.entity.v2.CashupPaymentSummaryEntity;
import za.co.mawa.bes.entity.v2.CashupReceiptEntity;
import za.co.mawa.bes.repository.v2.CashupDepositRepository;
import za.co.mawa.bes.repository.v2.CashupPaymentSummaryRepository;
import za.co.mawa.bes.repository.v2.CashupReceiptRepository;
import za.co.mawa.bes.repository.v2.CashupRepository;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service(value = "CashupServiceV2")
@RequiredArgsConstructor
public class CashupService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_AWAITING_DEPOSITS = "AWAITING_DEPOSITS";
    private static final String STATUS_COMPLETED = "COMPLETED"; // Legacy device-completed status
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final CashupRepository cashupRepository;
    private final CashupPaymentSummaryRepository cashupPaymentSummaryRepository;
    private final CashupReceiptRepository cashupReceiptRepository;
    private final CashupDepositRepository cashupDepositRepository;
    private final ApprovalService approvalService;
    private final Gson gson;

    /**
     * Upserts the cashup received from the offline Flutter app.
     *
     * New MawaPay flow:
     * 1. Device always has an active/open cashup.
     * 2. Every receipt is attached to the active cashup immediately.
     * 3. The app keeps syncing the same cashup while it is OPEN.
     * 4. When the cashier closes the cashup on the device, the same cashup is synced as AWAITING_DEPOSITS.
     *
     * Because of this, duplicate cashupNo must NOT be treated as an error. It is the normal
     * continuous-sync path and should update the existing portal cashup.
     */
    @Transactional
    public CashupResponse submitCashup(CashupRequest request) {
        validateRequest(request);

        CashupEntity cashup = cashupRepository.findByCashupNo(request.getCashupNo())
                .orElseGet(CashupEntity::new);

        boolean created = cashup.getId() == null;
        String requestedStatus = resolveStatus(request);

        if (!created && isLocked(cashup)) {
            return CashupResponse.builder()
                    .status("IGNORED")
                    .cashupId(cashup.getId())
                    .cashupNo(cashup.getCashupNo())
                    .message("Cashup is " + cashup.getStatus() + " and cannot be updated from device sync")
                    .build();
        }

        if (!created && isClosedForDeviceSync(cashup) && STATUS_OPEN.equals(requestedStatus)) {
            return CashupResponse.builder()
                    .status("IGNORED")
                    .cashupId(cashup.getId())
                    .cashupNo(cashup.getCashupNo())
                    .message("Cashup is already awaiting deposits/submitted; stale OPEN device sync ignored")
                    .build();
        }

        applyRequestToCashup(cashup, request, requestedStatus, created);
        cashup = cashupRepository.save(cashup);

        replacePaymentSummaries(cashup, request.getAmountByMethod(), request.getCountByMethod());
        replaceReceipts(cashup, request.getReceipts());

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .message(created ? "Cashup created successfully" : "Cashup updated successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public CashupSummaryResponse getCashup(String id) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        List<CashupPaymentSummaryDto> payments = cashupPaymentSummaryRepository
                .findByCashupId(cashup.getId())
                .stream()
                .map(item -> CashupPaymentSummaryDto.builder()
                        .paymentMethod(item.getPaymentMethod())
                        .amountCents(item.getAmountCents())
                        .paymentCount(item.getPaymentCount())
                        .build())
                .toList();

        return CashupSummaryResponse.builder()
                .id(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .deviceId(cashup.getDeviceId())
                .userId(cashup.getUserId())
                .cashupDate(cashup.getCashupDate())
                .totalCents(cashup.getTotalCents())
                .receiptCount(cashup.getReceiptCount())
                .status(cashup.getStatus())
                .depositTotalCents(defaultLong(cashup.getDepositTotalCents()))
                .depositCount(defaultInt(cashup.getDepositCount()))
                .approvalRequestId(cashup.getApprovalRequestId())
                .payments(payments)
                .deposits(getDeposits(cashup.getId()))
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<CashupSummaryResponse> getActiveCashup(String deviceId, String userId) {
        return cashupRepository
                .findFirstByDeviceIdAndUserIdAndStatusOrderByCreatedAtDesc(deviceId, userId, STATUS_OPEN)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<CashupSummaryResponse> getCashupsByDevice(String deviceId) {
        return cashupRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashupSummaryResponse> getCashupsByUserAndDateRange(
            String userId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return cashupRepository
                .findByUserIdAndCashupDateBetweenOrderByCashupDateDesc(userId, fromDate, toDate)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashupSummaryResponse> getAll() {
        return cashupRepository
                .findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public CashupDepositResponse createDeposit(String cashupId, CashupDepositRequest request) {
        CashupEntity cashup = cashupRepository.findById(cashupId)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + cashupId));

        if (!canCaptureDeposit(cashup)) {
            throw new IllegalStateException("Deposits can only be created before the cashup is submitted for approval");
        }

        validateDepositRequest(request);

        CashupDepositEntity deposit = new CashupDepositEntity();
        deposit.setCashup(cashup);
        deposit.setDepositDate(parseDate(request.getDepositDate()));
        deposit.setAmountCents(defaultLong(request.getAmountCents()));
        deposit.setPaymentMethod(clean(request.getPaymentMethod()));
        deposit.setBankName(clean(request.getBankName()));
        deposit.setReferenceNo(clean(request.getReferenceNo()));
        deposit.setNotes(clean(request.getNotes()));
        deposit.setCreatedBy(clean(request.getCreatedBy()));
        deposit.setUpdatedBy(clean(request.getCreatedBy()));

        deposit = cashupDepositRepository.save(deposit);
        recalculateDeposits(cashup);

        return toDepositResponse(deposit);
    }

    @Transactional(readOnly = true)
    public List<CashupDepositResponse> getDeposits(String cashupId) {
        return cashupDepositRepository.findByCashupIdOrderByDepositDateDescCreatedAtDesc(cashupId)
                .stream()
                .map(this::toDepositResponse)
                .toList();
    }

    @Transactional
    public void deleteDeposit(String cashupId, String depositId) {
        CashupEntity cashup = cashupRepository.findById(cashupId)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + cashupId));

        CashupDepositEntity deposit = cashupDepositRepository.findById(depositId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit not found: " + depositId));

        if (deposit.getCashup() == null || !cashupId.equals(deposit.getCashup().getId())) {
            throw new IllegalArgumentException("Deposit does not belong to cashup: " + cashupId);
        }

        if (STATUS_APPROVED.equalsIgnoreCase(cashup.getStatus()) || STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Deposits cannot be deleted after cashup submission");
        }

        cashupDepositRepository.delete(deposit);
        recalculateDeposits(cashup);
    }

    @Transactional
    public CashupResponse submitForApproval(String id, CashupSubmitForApprovalRequest request) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        if (cashup.getApprovalRequestId() != null && !cashup.getApprovalRequestId().isBlank()) {
            return CashupResponse.builder()
                    .status("IGNORED")
                    .cashupId(cashup.getId())
                    .cashupNo(cashup.getCashupNo())
                    .approvalRequestId(cashup.getApprovalRequestId())
                    .message("Cashup already has an approval request")
                    .build();
        }

        if (!canSubmitForApproval(cashup)) {
            throw new IllegalStateException("Only cashups awaiting deposits/open/draft cashups can be submitted for approval");
        }

        recalculateDeposits(cashup);
        String requesterId = request != null && request.getRequesterId() != null && !request.getRequesterId().isBlank()
                ? request.getRequesterId()
                : cashup.getUserId();

        ApprovalSubmitRequest approvalRequest = new ApprovalSubmitRequest();
        approvalRequest.setApprovalType(ApprovalType.CASHUP);
        approvalRequest.setReferenceId(cashup.getId());
        approvalRequest.setReferenceNo(String.valueOf(cashup.getCashupNo()));
        approvalRequest.setTitle("Cashup Approval: #" + cashup.getCashupNo());
        approvalRequest.setDescription("Cashup submitted for approval. Total collected: "
                + defaultLong(cashup.getTotalCents())
                + " cents, deposits: " + defaultLong(cashup.getDepositTotalCents()) + " cents.");
        approvalRequest.setRequesterId(requesterId);
        approvalRequest.setPayloadJson(gson.toJson(toSummary(cashup)));

        ApprovalRequestResponse approvalResponse = approvalService.submitForApproval(approvalRequest);

        cashup.setStatus(STATUS_SUBMITTED);
        cashup.setApprovalRequestId(approvalResponse.getId());
        cashup.setUpdatedBy(requesterId);
        cashupRepository.save(cashup);

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .approvalRequestId(approvalResponse.getId())
                .message("Cashup submitted for approval")
                .build();
    }

    @Transactional
    public CashupResponse approveCashup(String id, String approvedBy) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        if (!STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Only submitted cashups can be approved");
        }

        cashup.setStatus(STATUS_APPROVED);
        cashup.setUpdatedBy(approvedBy);

        cashupRepository.save(cashup);

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .message("Cashup approved successfully")
                .build();
    }

    @Transactional
    public CashupResponse rejectCashup(String id, String rejectedBy, String reason) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        if (!STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Only submitted cashups can be rejected");
        }

        cashup.setStatus(STATUS_REJECTED);
        cashup.setNotes(reason);
        cashup.setUpdatedBy(rejectedBy);

        cashupRepository.save(cashup);

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .message("Cashup rejected successfully")
                .build();
    }

    private void applyRequestToCashup(CashupEntity cashup, CashupRequest request, String requestedStatus, boolean created) {
        cashup.setCashupNo(request.getCashupNo());
        cashup.setDeviceId(request.getDeviceId());
        cashup.setUserId(request.getUserId());
        cashup.setCashupDate(parseDate(request.getDate()));
        cashup.setTotalCents(defaultLong(request.getTotalCents()));
        cashup.setReceiptCount(defaultInt(request.getReceiptCount()));
        cashup.setStatus(requestedStatus);
        cashup.setNotes(request.getNotes());
        cashup.setSyncedAt(LocalDateTime.now());

        if (created) {
            cashup.setCreatedBy(request.getUserId());
        }
        cashup.setUpdatedBy(request.getUserId());
    }

    private void replacePaymentSummaries(
            CashupEntity cashup,
            Map<String, Long> amountByMethod,
            Map<String, Integer> countByMethod
    ) {
        List<CashupPaymentSummaryEntity> existingSummaries =
                cashupPaymentSummaryRepository.findByCashupId(cashup.getId());

        Map<String, CashupPaymentSummaryEntity> existingByMethod = new HashMap<>();
        for (CashupPaymentSummaryEntity existing : existingSummaries) {
            String paymentMethod = normalizePaymentMethod(existing.getPaymentMethod());
            if (paymentMethod != null) {
                existingByMethod.put(paymentMethod, existing);
            }
        }

        if (amountByMethod == null || amountByMethod.isEmpty()) {
            if (!existingSummaries.isEmpty()) {
                cashupPaymentSummaryRepository.deleteAll(existingSummaries);
            }
            return;
        }

        List<CashupPaymentSummaryEntity> summariesToSave = new ArrayList<>();

        amountByMethod.forEach((method, amount) -> {
            String paymentMethod = normalizePaymentMethod(method);
            if (paymentMethod == null) {
                return;
            }

            CashupPaymentSummaryEntity entity = existingByMethod.remove(paymentMethod);
            if (entity == null) {
                entity = new CashupPaymentSummaryEntity();
                entity.setCashup(cashup);
                entity.setPaymentMethod(paymentMethod);
            }

            entity.setAmountCents(defaultLong(amount));
            entity.setPaymentCount(defaultInt(resolvePaymentCount(countByMethod, method, paymentMethod)));

            summariesToSave.add(entity);
        });

        if (!existingByMethod.isEmpty()) {
            cashupPaymentSummaryRepository.deleteAll(existingByMethod.values());
        }

        if (!summariesToSave.isEmpty()) {
            cashupPaymentSummaryRepository.saveAll(summariesToSave);
        }
    }

    private Integer resolvePaymentCount(Map<String, Integer> countByMethod, String originalMethod, String normalizedMethod) {
        if (countByMethod == null || countByMethod.isEmpty()) {
            return null;
        }

        Integer count = countByMethod.get(originalMethod);
        if (count != null) {
            return count;
        }

        count = countByMethod.get(normalizedMethod);
        if (count != null) {
            return count;
        }

        for (Map.Entry<String, Integer> entry : countByMethod.entrySet()) {
            if (normalizedMethod.equals(normalizePaymentMethod(entry.getKey()))) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String normalizePaymentMethod(String method) {
        String value = clean(method);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private void replaceReceipts(CashupEntity cashup, List<CashupReceiptRequest> receipts) {
        cashupReceiptRepository.deleteByCashupId(cashup.getId());

        if (receipts == null || receipts.isEmpty()) {
            return;
        }

        List<CashupReceiptEntity> entities = receipts.stream()
                .map(item -> {
                    CashupReceiptEntity entity = new CashupReceiptEntity();
                    entity.setCashup(cashup);
                    entity.setReceiptId(item.getReceiptId());
                    entity.setReceiptNo(item.getReceiptNo());
                    entity.setAmountCents(defaultLong(item.getAmountCents()));
                    entity.setPaymentMethod(item.getPaymentMethod());
                    return entity;
                })
                .toList();

        cashupReceiptRepository.saveAll(entities);
    }

    private CashupSummaryResponse toSummary(CashupEntity cashup) {
        return CashupSummaryResponse.builder()
                .id(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .deviceId(cashup.getDeviceId())
                .userId(cashup.getUserId())
                .cashupDate(cashup.getCashupDate())
                .totalCents(cashup.getTotalCents())
                .receiptCount(cashup.getReceiptCount())
                .status(cashup.getStatus())
                .depositTotalCents(defaultLong(cashup.getDepositTotalCents()))
                .depositCount(defaultInt(cashup.getDepositCount()))
                .approvalRequestId(cashup.getApprovalRequestId())
                .deposits(getDeposits(cashup.getId()))
                .build();
    }

    @Transactional
    public void markSubmittedFromApproval(String cashupId, String approvalRequestId, String actionBy) {
        CashupEntity cashup = cashupRepository.findById(cashupId)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + cashupId));
        cashup.setStatus(STATUS_SUBMITTED);
        cashup.setApprovalRequestId(approvalRequestId);
        cashup.setUpdatedBy(actionBy);
        cashupRepository.save(cashup);
    }

    @Transactional
    public void markApprovedFromApproval(String cashupId, String actionBy) {
        CashupEntity cashup = cashupRepository.findById(cashupId)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + cashupId));
        cashup.setStatus(STATUS_APPROVED);
        cashup.setUpdatedBy(actionBy);
        cashupRepository.save(cashup);
    }

    private void validateDepositRequest(CashupDepositRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Deposit request is required");
        }
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("amountCents must be greater than zero");
        }
        if (request.getDepositDate() == null || request.getDepositDate().isBlank()) {
            throw new IllegalArgumentException("depositDate is required");
        }
    }

    private void recalculateDeposits(CashupEntity cashup) {
        List<CashupDepositEntity> deposits = cashupDepositRepository.findByCashupIdOrderByDepositDateDescCreatedAtDesc(cashup.getId());
        long total = deposits.stream().mapToLong(item -> defaultLong(item.getAmountCents())).sum();
        cashup.setDepositTotalCents(total);
        cashup.setDepositCount(deposits.size());
        cashupRepository.save(cashup);
    }

    private CashupDepositResponse toDepositResponse(CashupDepositEntity entity) {
        return CashupDepositResponse.builder()
                .id(entity.getId())
                .cashupId(entity.getCashup() != null ? entity.getCashup().getId() : null)
                .depositDate(entity.getDepositDate())
                .amountCents(defaultLong(entity.getAmountCents()))
                .paymentMethod(entity.getPaymentMethod())
                .bankName(entity.getBankName())
                .referenceNo(entity.getReferenceNo())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateRequest(CashupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Cashup request is required");
        }

        if (request.getCashupNo() == null) {
            throw new IllegalArgumentException("cashupNo is required");
        }

        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        if (request.getDate() == null || request.getDate().isBlank()) {
            throw new IllegalArgumentException("date is required");
        }

        if (request.getTotalCents() == null) {
            throw new IllegalArgumentException("totalCents is required");
        }

        if (request.getReceiptCount() == null) {
            throw new IllegalArgumentException("receiptCount is required");
        }
    }

    private String resolveStatus(CashupRequest request) {
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String status = request.getStatus().trim().toUpperCase();
            if (STATUS_OPEN.equals(status)) {
                return STATUS_OPEN;
            }
            if (STATUS_AWAITING_DEPOSITS.equals(status) || "DEPOSIT_PENDING".equals(status)) {
                return STATUS_AWAITING_DEPOSITS;
            }
            if (STATUS_COMPLETED.equals(status) || "CLOSED".equals(status)) {
                // Backwards compatibility: earlier MAWAPay builds used COMPLETED to mean
                // "cashier closed the device cashup". In ERP this must be the deposit stage.
                return STATUS_AWAITING_DEPOSITS;
            }
            if (STATUS_SUBMITTED.equals(status)) {
                // Backwards compatibility: older MAWAPay clients used SUBMITTED to mean
                // "cashier closed the device cashup". In ERP this must still be editable
                // for deposits before it is submitted for approval.
                return STATUS_AWAITING_DEPOSITS;
            }
            throw new IllegalArgumentException("Invalid cashup status from device: " + request.getStatus());
        }

        String notes = request.getNotes() == null ? "" : request.getNotes().toUpperCase();
        if (notes.contains("LOCAL STATUS: OPEN") || notes.contains("LOCAL STATUS: ACTIVE_CASHUP")) {
            return STATUS_OPEN;
        }
        if (notes.contains("LOCAL STATUS: AWAITING_DEPOSITS")
                || notes.contains("LOCAL STATUS: DEPOSIT_PENDING")
                || notes.contains("LOCAL STATUS: COMPLETED")
                || notes.contains("LOCAL STATUS: SUBMITTED")
                || notes.contains("LOCAL STATUS: CLOSED")) {
            return STATUS_AWAITING_DEPOSITS;
        }

        // Backwards compatibility for older clients that used this endpoint only at final cashup time.
        return STATUS_AWAITING_DEPOSITS;
    }

    private boolean isLocked(CashupEntity cashup) {
        return STATUS_APPROVED.equalsIgnoreCase(cashup.getStatus())
                || STATUS_REJECTED.equalsIgnoreCase(cashup.getStatus());
    }

    private boolean isClosedForDeviceSync(CashupEntity cashup) {
        return STATUS_AWAITING_DEPOSITS.equalsIgnoreCase(cashup.getStatus())
                || STATUS_COMPLETED.equalsIgnoreCase(cashup.getStatus())
                || STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus());
    }

    private boolean canCaptureDeposit(CashupEntity cashup) {
        String status = cashup.getStatus() == null ? "" : cashup.getStatus().trim().toUpperCase(Locale.ROOT);
        return STATUS_AWAITING_DEPOSITS.equals(status)
                || STATUS_COMPLETED.equals(status) // Legacy compatibility for already-synced cashups
                || STATUS_OPEN.equals(status)
                || "DRAFT".equals(status)
                || "NEW".equals(status);
    }

    private boolean canSubmitForApproval(CashupEntity cashup) {
        return canCaptureDeposit(cashup);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // Continue below
        }

        try {
            return Instant.parse(value)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Continue below
        }

        try {
            return LocalDateTime.parse(value)
                    .toLocalDate();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid cashup date: " + value);
        }
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
