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

@Service(value = "CashupServiceV2")
@RequiredArgsConstructor
public class CashupService {

    private final CashupRepository cashupRepository;
    private final CashupPaymentSummaryRepository cashupPaymentSummaryRepository;
    private final CashupReceiptRepository cashupReceiptRepository;

    @Transactional
    public CashupResponse submitCashup(CashupRequest request) {
        validateRequest(request);

        if (cashupRepository.existsByCashupNo(request.getCashupNo())) {
            CashupEntity existing = cashupRepository
                    .findByCashupNo(request.getCashupNo())
                    .orElseThrow();

            return CashupResponse.builder()
                    .status("SUCCESS")
                    .cashupId(existing.getId())
                    .cashupNo(existing.getCashupNo())
                    .message("Cashup already submitted")
                    .build();
        }

        CashupEntity cashup = new CashupEntity();
        cashup.setCashupNo(request.getCashupNo());
        cashup.setDeviceId(request.getDeviceId());
        cashup.setUserId(request.getUserId());
        cashup.setCashupDate(parseDate(request.getDate()));
        cashup.setTotalCents(defaultLong(request.getTotalCents()));
        cashup.setReceiptCount(defaultInt(request.getReceiptCount()));
        cashup.setStatus("SUBMITTED");
        cashup.setNotes(request.getNotes());
        cashup.setSyncedAt(LocalDateTime.now());
        cashup.setCreatedBy(request.getUserId());

        cashup = cashupRepository.save(cashup);

        savePaymentSummaries(cashup, request.getAmountByMethod(), request.getCountByMethod());
        saveReceipts(cashup, request.getReceipts());

        return CashupResponse.builder()
                .status("SUCCESS")
                .cashupId(cashup.getId())
                .cashupNo(cashup.getCashupNo())
                .message("Cashup submitted successfully")
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

    @Transactional
    public CashupResponse approveCashup(String id, String approvedBy) {
        CashupEntity cashup = cashupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + id));

        if (!"SUBMITTED".equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Only submitted cashups can be approved");
        }

        cashup.setStatus("APPROVED");
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

        if (!"SUBMITTED".equalsIgnoreCase(cashup.getStatus())) {
            throw new IllegalStateException("Only submitted cashups can be rejected");
        }

        cashup.setStatus("REJECTED");
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

    private void savePaymentSummaries(
            CashupEntity cashup,
            Map<String, Long> amountByMethod,
            Map<String, Integer> countByMethod
    ) {
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

    private void saveReceipts(CashupEntity cashup, List<CashupReceiptRequest> receipts) {
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
