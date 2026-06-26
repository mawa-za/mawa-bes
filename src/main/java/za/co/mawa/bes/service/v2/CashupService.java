package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.payapp.*;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.entity.v2.CashupPaymentSummaryEntity;
import za.co.mawa.bes.entity.v2.CashupReceiptEntity;
import za.co.mawa.bes.repository.v2.CashupPaymentSummaryRepository;
import za.co.mawa.bes.repository.v2.CashupReceiptRepository;
import za.co.mawa.bes.repository.v2.CashupRepository;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service(value = "CashupServiceV2")
@RequiredArgsConstructor
public class CashupService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final CashupRepository cashupRepository;
    private final CashupPaymentSummaryRepository cashupPaymentSummaryRepository;
    private final CashupReceiptRepository cashupReceiptRepository;

    /**
     * Upserts the cashup received from the offline Flutter app.
     *
     * New MawaPay flow:
     * 1. Device always has an active/open cashup.
     * 2. Every receipt is attached to the active cashup immediately.
     * 3. The app keeps syncing the same cashup while it is OPEN.
     * 4. When the cashier submits/closes the cashup, the same cashup is synced as SUBMITTED.
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

        if (!created && STATUS_SUBMITTED.equalsIgnoreCase(cashup.getStatus()) && STATUS_OPEN.equals(requestedStatus)) {
            return CashupResponse.builder()
                    .status("IGNORED")
                    .cashupId(cashup.getId())
                    .cashupNo(cashup.getCashupNo())
                    .message("Cashup is already submitted; stale OPEN device sync ignored")
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
                .payments(payments)
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
        cashupPaymentSummaryRepository.deleteByCashupId(cashup.getId());

        if (amountByMethod == null || amountByMethod.isEmpty()) {
            return;
        }

        List<CashupPaymentSummaryEntity> summaries = new ArrayList<>();

        amountByMethod.forEach((method, amount) -> {
            CashupPaymentSummaryEntity entity = new CashupPaymentSummaryEntity();
            entity.setCashup(cashup);
            entity.setPaymentMethod(method);
            entity.setAmountCents(defaultLong(amount));

            Integer count = countByMethod != null ? countByMethod.get(method) : null;
            entity.setPaymentCount(defaultInt(count));

            summaries.add(entity);
        });

        cashupPaymentSummaryRepository.saveAll(summaries);
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
                .build();
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
            if (STATUS_OPEN.equals(status) || STATUS_SUBMITTED.equals(status)) {
                return status;
            }
            throw new IllegalArgumentException("Invalid cashup status from device: " + request.getStatus());
        }

        String notes = request.getNotes() == null ? "" : request.getNotes().toUpperCase();
        if (notes.contains("LOCAL STATUS: OPEN") || notes.contains("LOCAL STATUS: ACTIVE_CASHUP")) {
            return STATUS_OPEN;
        }
        if (notes.contains("LOCAL STATUS: SUBMITTED") || notes.contains("LOCAL STATUS: CLOSED")) {
            return STATUS_SUBMITTED;
        }

        // Backwards compatibility for older clients that used this endpoint only at final cashup time.
        return STATUS_SUBMITTED;
    }

    private boolean isLocked(CashupEntity cashup) {
        return STATUS_APPROVED.equalsIgnoreCase(cashup.getStatus())
                || STATUS_REJECTED.equalsIgnoreCase(cashup.getStatus());
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
