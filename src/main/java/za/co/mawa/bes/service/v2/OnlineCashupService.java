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
    private static final String STATUS_OPEN = "OPEN";
    private static final String DEFAULT_DEVICE = "ERP-ONLINE";
    private static final String DEFAULT_USER = "SYSTEM";

    private final CashupRepository cashupRepository;
    private final CashupReceiptRepository cashupReceiptRepository;
    private final CashupPaymentSummaryRepository cashupPaymentSummaryRepository;
    private final ReceiptRepository receiptRepository;
    private final NumberAllocationService numberAllocationService;

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
