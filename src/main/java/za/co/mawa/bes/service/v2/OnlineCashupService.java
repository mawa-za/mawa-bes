package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.entity.v2.CashupPaymentSummaryEntity;
import za.co.mawa.bes.entity.v2.CashupReceiptEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.repository.v2.CashupPaymentSummaryRepository;
import za.co.mawa.bes.repository.v2.CashupReceiptRepository;
import za.co.mawa.bes.repository.v2.CashupRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared cashup posting for payments processed directly in MawaERP.
 *
 * Manual receipt-book captures intentionally do not call this service because
 * those receipts represent money already collected outside the online ERP flow.
 */
@Service
@RequiredArgsConstructor
public class OnlineCashupService {

    private static final String SOURCE = "ERP_ONLINE";
    private static final String SOURCE_EFT = "ERP_ONLINE_EFT";
    private static final String STATUS_OPEN = "OPEN";
    private static final String DEFAULT_DEVICE = "ERP-ONLINE";
    private static final String DEFAULT_USER = "SYSTEM";

    private final CashupRepository cashupRepository;
    private final CashupReceiptRepository cashupReceiptRepository;
    private final CashupPaymentSummaryRepository cashupPaymentSummaryRepository;
    private final ReceiptRepository receiptRepository;
    private final NumberAllocationService numberAllocationService;
    private final CashupService cashupService;

    @Transactional
    public void addReceipts(
            PaymentBatchEntity batch,
            Collection<String> receiptIds,
            String actor,
            String deviceId
    ) {
        if (batch == null || receiptIds == null || receiptIds.isEmpty()) {
            return;
        }

        List<String> uniqueReceiptIds = receiptIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        if (uniqueReceiptIds.isEmpty()) {
            return;
        }

        List<ReceiptEntity> receipts = receiptRepository.findAllById(uniqueReceiptIds);
        if (receipts.isEmpty()) {
            return;
        }

        String user = firstNonBlank(actor, batch.getCreatedBy(), DEFAULT_USER);
        String device = firstNonBlank(deviceId, batch.getDeviceId(), DEFAULT_DEVICE);
        String paymentMethod = firstNonBlank(batch.getPaymentMethod(), receipts.get(0).getPaymentMethod(), "OTHER")
                .toUpperCase(Locale.ROOT);

        if (isIndividualEftPayment(paymentMethod)) {
            addEftPaymentCashup(batch, receipts, user, device, paymentMethod);
            return;
        }

        CashupEntity cashup = cashupRepository
                .findFirstByDeviceIdAndUserIdAndStatusAndSourceOrderByCreatedAtDesc(
                        device, user, STATUS_OPEN, SOURCE)
                .orElseGet(() -> createCashup(device, user));

        long addedAmountCents = 0L;
        int addedReceiptCount = 0;
        for (ReceiptEntity receipt : receipts) {
            if (receipt == null || receipt.getId() == null
                    || cashupReceiptRepository.existsByCashupIdAndReceiptId(cashup.getId(), receipt.getId())) {
                continue;
            }

            long receiptAmount = value(receipt.getTotalAmountCents());
            CashupReceiptEntity cashupReceipt = new CashupReceiptEntity();
            cashupReceipt.setCashup(cashup);
            cashupReceipt.setReceiptId(receipt.getId());
            cashupReceipt.setAmountCents(receiptAmount);
            cashupReceipt.setPaymentMethod(firstNonBlank(receipt.getPaymentMethod(), paymentMethod));
            cashupReceipt.setLegacyTransactionId(batch.getId());
            cashupReceiptRepository.save(cashupReceipt);

            addedAmountCents += receiptAmount;
            addedReceiptCount++;
        }

        if (addedReceiptCount == 0) {
            return;
        }

        cashup.setTotalCents(value(cashup.getTotalCents()) + addedAmountCents);
        cashup.setReceiptCount(value(cashup.getReceiptCount()) + addedReceiptCount);
        cashup.setUpdatedBy(user);
        cashupRepository.save(cashup);

        CashupPaymentSummaryEntity summary = cashupPaymentSummaryRepository.findByCashupId(cashup.getId())
                .stream()
                .filter(item -> paymentMethod.equalsIgnoreCase(item.getPaymentMethod()))
                .findFirst()
                .orElseGet(CashupPaymentSummaryEntity::new);
        summary.setCashup(cashup);
        summary.setPaymentMethod(paymentMethod);
        summary.setAmountCents(value(summary.getAmountCents()) + addedAmountCents);
        summary.setPaymentCount(value(summary.getPaymentCount()) + addedReceiptCount);
        cashupPaymentSummaryRepository.save(summary);
    }

    private void addEftPaymentCashup(
            PaymentBatchEntity batch,
            List<ReceiptEntity> receipts,
            String user,
            String device,
            String paymentMethod
    ) {
        CashupEntity cashup = cashupRepository
                .findFirstByLegacyTransactionIdAndSourceOrderByCreatedAtDesc(batch.getId(), SOURCE_EFT)
                .orElseGet(() -> createEftCashup(batch, device, user));

        // A retry of the payment-posting call must not create a second cashup or approval request.
        // Add any missing receipt links first, then rebuild the totals from the authoritative links.
        for (ReceiptEntity receipt : receipts) {
            if (receipt == null || receipt.getId() == null
                    || cashupReceiptRepository.existsByCashupIdAndReceiptId(cashup.getId(), receipt.getId())) {
                continue;
            }

            CashupReceiptEntity cashupReceipt = new CashupReceiptEntity();
            cashupReceipt.setCashup(cashup);
            cashupReceipt.setReceiptId(receipt.getId());
            cashupReceipt.setAmountCents(value(receipt.getTotalAmountCents()));
            cashupReceipt.setPaymentMethod(firstNonBlank(receipt.getPaymentMethod(), paymentMethod));
            cashupReceipt.setLegacyTransactionId(batch.getId());
            cashupReceiptRepository.save(cashupReceipt);
        }

        List<CashupReceiptEntity> links = cashupReceiptRepository.findByCashupId(cashup.getId());
        long totalCents = links.stream().mapToLong(item -> value(item.getAmountCents())).sum();
        cashup.setTotalCents(totalCents);
        cashup.setReceiptCount(links.size());
        cashup.setDepositTotalCents(0L);
        cashup.setDepositCount(0);
        cashup.setUpdatedBy(user);
        cashupRepository.save(cashup);

        cashupPaymentSummaryRepository.deleteByCashupId(cashup.getId());
        cashupPaymentSummaryRepository.flush();
        CashupPaymentSummaryEntity summary = new CashupPaymentSummaryEntity();
        summary.setCashup(cashup);
        summary.setPaymentMethod(paymentMethod);
        summary.setAmountCents(totalCents);
        // This cashup represents one processed payment, even when that payment allocated to multiple receipts.
        summary.setPaymentCount(1);
        cashupPaymentSummaryRepository.save(summary);

        if (cashup.getApprovalRequestId() == null || cashup.getApprovalRequestId().isBlank()) {
            za.co.mawa.bes.dto.v2.payapp.CashupSubmitForApprovalRequest submitRequest =
                    new za.co.mawa.bes.dto.v2.payapp.CashupSubmitForApprovalRequest();
            submitRequest.setRequesterId(user);
            submitRequest.setComments("Automatically submitted after " + paymentMethod
                    + " payment " + batch.getPaymentBatchNo() + " was processed. Deposit not required.");
            cashupService.submitForApproval(cashup.getId(), submitRequest);
        }
    }

    private CashupEntity createEftCashup(PaymentBatchEntity batch, String device, String user) {
        CashupEntity cashup = new CashupEntity();
        cashup.setCashupNo(Long.parseLong(numberAllocationService.allocateNumber("CASHUP")));
        cashup.setDeviceId(device);
        cashup.setUserId(user);
        cashup.setCashupDate(batch.getPaymentDate() == null ? LocalDate.now() : batch.getPaymentDate().toLocalDate());
        cashup.setStatus(STATUS_OPEN);
        cashup.setSource(SOURCE_EFT);
        cashup.setLegacyTransactionId(batch.getId());
        cashup.setNotes("Individual EFT cashup for payment batch " + batch.getPaymentBatchNo()
                + ". Deposit not required.");
        cashup.setCreatedBy(user);
        cashup.setTotalCents(0L);
        cashup.setReceiptCount(0);
        cashup.setDepositTotalCents(0L);
        cashup.setDepositCount(0);
        return cashupRepository.save(cashup);
    }

    private boolean isIndividualEftPayment(String paymentMethod) {
        return "EFT".equalsIgnoreCase(paymentMethod);
    }

    @Transactional
    public void removeReceipts(Collection<ReceiptEntity> receipts, String actor) {
        removeReceipts(receipts, actor, true);
    }

    @Transactional
    public void removeReceipts(
            Collection<ReceiptEntity> receipts,
            String actor,
            boolean validateCashupStatus
    ) {
        removeReceipts(receipts, List.of(), actor, validateCashupStatus);
    }

    @Transactional
    public void removeReceipts(
            Collection<ReceiptEntity> receipts,
            Collection<String> additionalReceiptIds,
            String actor,
            boolean validateCashupStatus
    ) {
        removeReceipts(receipts, additionalReceiptIds, actor, validateCashupStatus, false);
    }

    @Transactional
    public void removeReceipts(
            Collection<ReceiptEntity> receipts,
            Collection<String> additionalReceiptIds,
            String actor,
            boolean validateCashupStatus,
            boolean allowMissingCashupLink
    ) {
        if ((receipts == null || receipts.isEmpty())
                && (additionalReceiptIds == null || additionalReceiptIds.isEmpty())) return;

        Map<String, CashupEntity> affectedCashups = new LinkedHashMap<>();
        Map<String, CashupReceiptEntity> linksById = new LinkedHashMap<>();
        for (ReceiptEntity receipt : receipts) {
            if (receipt == null) continue;
            if (receipt.getId() != null) {
                for (CashupReceiptEntity link : cashupReceiptRepository.findByReceiptId(receipt.getId())) {
                    linksById.put(link.getId(), link);
                }
            }
            if (receipt.getPaymentBatchId() != null && !receipt.getPaymentBatchId().isBlank()) {
                for (CashupReceiptEntity link : cashupReceiptRepository
                        .findByLegacyTransactionId(receipt.getPaymentBatchId().trim())) {
                    linksById.put(link.getId(), link);
                }
            }
            for (String receiptNumber : List.of(
                    receipt.getReceiptNo() == null ? "" : receipt.getReceiptNo(),
                    receipt.getManualReceiptNo() == null ? "" : receipt.getManualReceiptNo(),
                    receipt.getExternalReceiptNo() == null ? "" : receipt.getExternalReceiptNo())) {
                Long numericReceiptNo = numericReceiptNo(receiptNumber);
                if (numericReceiptNo == null) continue;
                for (CashupReceiptEntity link : cashupReceiptRepository.findByReceiptNo(numericReceiptNo)) {
                    linksById.put(link.getId(), link);
                }
            }
        }
        if (additionalReceiptIds != null) {
            for (String additionalReceiptId : additionalReceiptIds) {
                if (additionalReceiptId == null || additionalReceiptId.isBlank()) continue;
                for (CashupReceiptEntity link : cashupReceiptRepository.findByReceiptId(additionalReceiptId.trim())) {
                    linksById.put(link.getId(), link);
                }
            }
        }

        List<CashupReceiptEntity> links = List.copyOf(linksById.values());

        if (links.isEmpty()) {
            if (validateCashupStatus && !allowMissingCashupLink) {
                throw new IllegalStateException("The payment is not linked to an open cash-up");
            }
            return;
        }

        for (CashupReceiptEntity link : links) {
            CashupEntity cashup = link.getCashup();
            if (cashup == null) {
                if (validateCashupStatus) {
                    throw new IllegalStateException("The payment has an invalid cash-up link");
                }
                continue;
            }
            if (validateCashupStatus && !STATUS_OPEN.equalsIgnoreCase(cashup.getStatus())) {
                throw new IllegalStateException("Premium payments can only be deleted while every linked cash-up is OPEN");
            }
            affectedCashups.put(cashup.getId(), cashup);
        }

        cashupReceiptRepository.deleteAll(links);
        cashupReceiptRepository.flush();

        String user = firstNonBlank(actor, DEFAULT_USER);
        for (CashupEntity cashup : affectedCashups.values()) {
            rebuildCashupFromLinks(cashup, user);
        }
    }

    @Transactional
    public void refreshReceiptAmount(
            ReceiptEntity receipt,
            String additionalReceiptId,
            String actor
    ) {
        if (receipt == null || receipt.getId() == null) return;

        Map<String, CashupReceiptEntity> linksById = new LinkedHashMap<>();
        for (CashupReceiptEntity link : cashupReceiptRepository.findByReceiptId(receipt.getId())) {
            linksById.put(link.getId(), link);
        }
        if (additionalReceiptId != null && !additionalReceiptId.isBlank()) {
            for (CashupReceiptEntity link : cashupReceiptRepository.findByReceiptId(additionalReceiptId.trim())) {
                linksById.put(link.getId(), link);
            }
        }
        if (linksById.isEmpty()) return;

        Map<String, CashupEntity> affectedCashups = new LinkedHashMap<>();
        long amount = value(receipt.getTotalAmountCents());
        for (CashupReceiptEntity link : linksById.values()) {
            link.setAmountCents(amount);
            if (receipt.getPaymentMethod() != null && !receipt.getPaymentMethod().isBlank()) {
                link.setPaymentMethod(receipt.getPaymentMethod().trim());
            }
            cashupReceiptRepository.save(link);
            if (link.getCashup() != null) affectedCashups.put(link.getCashup().getId(), link.getCashup());
        }
        cashupReceiptRepository.flush();

        String user = firstNonBlank(actor, DEFAULT_USER);
        for (CashupEntity cashup : affectedCashups.values()) {
            rebuildCashupFromLinks(cashup, user);
        }
    }

    private void rebuildCashupFromLinks(CashupEntity cashup, String actor) {
        List<CashupReceiptEntity> remaining = cashupReceiptRepository.findByCashupId(cashup.getId());
        long receiptTotal = remaining.stream().mapToLong(item -> value(item.getAmountCents())).sum();
        boolean manual = "MANUAL_RECEIPT_BOOK".equalsIgnoreCase(cashup.getSource());
        if (manual) {
            long declared = value(cashup.getManualAmountCents());
            cashup.setTotalCents(declared);
            int declaredCount = value(cashup.getReceiptCount());
            if (declaredCount > 0 && remaining.size() == declaredCount) {
                cashup.setReceiptTotalCents(receiptTotal);
                cashup.setVarianceCents(declared - receiptTotal);
            } else {
                cashup.setReceiptTotalCents(declared);
                cashup.setVarianceCents(0L);
            }
        } else {
            cashup.setTotalCents(receiptTotal);
            cashup.setReceiptCount(remaining.size());
        }
        cashup.setUpdatedBy(actor);
        cashupRepository.save(cashup);

        cashupPaymentSummaryRepository.deleteByCashupId(cashup.getId());
        cashupPaymentSummaryRepository.flush();
        Map<String, List<CashupReceiptEntity>> byMethod = remaining.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> firstNonBlank(item.getPaymentMethod(), "OTHER").toUpperCase(Locale.ROOT),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        for (Map.Entry<String, List<CashupReceiptEntity>> entry : byMethod.entrySet()) {
            CashupPaymentSummaryEntity summary = new CashupPaymentSummaryEntity();
            summary.setCashup(cashup);
            summary.setPaymentMethod(entry.getKey());
            summary.setAmountCents(entry.getValue().stream()
                    .mapToLong(item -> value(item.getAmountCents())).sum());
            summary.setPaymentCount(entry.getValue().size());
            cashupPaymentSummaryRepository.save(summary);
        }
    }

    private CashupEntity createCashup(String device, String user) {
        CashupEntity cashup = new CashupEntity();
        cashup.setCashupNo(Long.parseLong(numberAllocationService.allocateNumber("CASHUP")));
        cashup.setDeviceId(device);
        cashup.setUserId(user);
        cashup.setCashupDate(LocalDate.now());
        cashup.setStatus(STATUS_OPEN);
        cashup.setSource(SOURCE);
        cashup.setCreatedBy(user);
        cashup.setTotalCents(0L);
        cashup.setReceiptCount(0);
        cashup.setDepositTotalCents(0L);
        cashup.setDepositCount(0);
        return cashupRepository.save(cashup);
    }

    private static Long numericReceiptNo(String receiptNo) {
        if (receiptNo == null || receiptNo.isBlank()) return null;
        String digits = receiptNo.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return Long.valueOf(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static long value(Long amount) {
        return amount == null ? 0L : amount;
    }

    private static int value(Integer count) {
        return count == null ? 0 : count;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
