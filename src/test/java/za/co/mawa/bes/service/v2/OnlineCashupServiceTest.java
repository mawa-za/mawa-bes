package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.v2.payapp.CashupSubmitForApprovalRequest;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.entity.v2.CashupPaymentSummaryEntity;
import za.co.mawa.bes.entity.v2.CashupReceiptEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.repository.v2.CashupPaymentSummaryRepository;
import za.co.mawa.bes.repository.v2.CashupReceiptRepository;
import za.co.mawa.bes.repository.v2.CashupRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineCashupServiceTest {

    @Mock private CashupRepository cashupRepository;
    @Mock private CashupReceiptRepository cashupReceiptRepository;
    @Mock private CashupPaymentSummaryRepository cashupPaymentSummaryRepository;
    @Mock private ReceiptRepository receiptRepository;
    @Mock private NumberAllocationService numberAllocationService;
    @Mock private CashupService cashupService;

    @InjectMocks
    private OnlineCashupService service;

    @Test
    void cardPaymentUsesNormalConsolidatedCashup() {
        PaymentBatchEntity batch = PaymentBatchEntity.builder()
                .id("batch-card")
                .paymentBatchNo("PB-CARD")
                .paymentMethod("CARD")
                .paymentDate(LocalDateTime.of(2026, 8, 22, 10, 0))
                .deviceId("device-1")
                .createdBy("cashier-1")
                .build();
        ReceiptEntity receipt = ReceiptEntity.builder()
                .id("receipt-card")
                .paymentMethod("CARD")
                .totalAmountCents(12_500L)
                .build();
        CashupEntity cashup = CashupEntity.builder()
                .id("cashup-normal")
                .cashupNo(1001L)
                .deviceId("device-1")
                .userId("cashier-1")
                .status("OPEN")
                .source("ERP_ONLINE")
                .totalCents(0L)
                .receiptCount(0)
                .build();

        when(receiptRepository.findAllById(any())).thenReturn(List.of(receipt));
        when(cashupRepository.findFirstByDeviceIdAndUserIdAndStatusAndSourceOrderByCreatedAtDesc(
                "device-1", "cashier-1", "OPEN", "ERP_ONLINE"))
                .thenReturn(Optional.of(cashup));
        when(cashupReceiptRepository.existsByCashupIdAndReceiptId("cashup-normal", "receipt-card"))
                .thenReturn(false);
        when(cashupPaymentSummaryRepository.findByCashupId("cashup-normal"))
                .thenReturn(List.of());

        service.addReceipts(batch, List.of("receipt-card"), "cashier-1", "device-1");

        verify(cashupRepository, never())
                .findFirstByLegacyTransactionIdAndSourceOrderByCreatedAtDesc(any(), eq("ERP_ONLINE_EFT"));
        verify(cashupService, never()).submitForApproval(any(), any(CashupSubmitForApprovalRequest.class));

        ArgumentCaptor<CashupPaymentSummaryEntity> summaryCaptor =
                ArgumentCaptor.forClass(CashupPaymentSummaryEntity.class);
        verify(cashupPaymentSummaryRepository).save(summaryCaptor.capture());
        assertEquals("CARD", summaryCaptor.getValue().getPaymentMethod());
        assertEquals(12_500L, summaryCaptor.getValue().getAmountCents());
        assertEquals(1, summaryCaptor.getValue().getPaymentCount());
        assertEquals(12_500L, cashup.getTotalCents());
        assertEquals(1, cashup.getReceiptCount());
    }

    @Test
    void eftPaymentUsesDedicatedCashupAndSubmitsForApproval() {
        PaymentBatchEntity batch = PaymentBatchEntity.builder()
                .id("batch-eft")
                .paymentBatchNo("PB-EFT")
                .paymentMethod("EFT")
                .paymentDate(LocalDateTime.of(2026, 8, 22, 10, 30))
                .deviceId("device-1")
                .createdBy("cashier-1")
                .build();
        ReceiptEntity receipt = ReceiptEntity.builder()
                .id("receipt-eft")
                .paymentMethod("EFT")
                .totalAmountCents(20_000L)
                .build();
        CashupEntity cashup = CashupEntity.builder()
                .id("cashup-eft")
                .cashupNo(1002L)
                .deviceId("device-1")
                .userId("cashier-1")
                .status("OPEN")
                .source("ERP_ONLINE_EFT")
                .legacyTransactionId("batch-eft")
                .totalCents(0L)
                .receiptCount(0)
                .depositTotalCents(0L)
                .depositCount(0)
                .build();
        CashupReceiptEntity linkedReceipt = CashupReceiptEntity.builder()
                .cashup(cashup)
                .receiptId("receipt-eft")
                .paymentMethod("EFT")
                .amountCents(20_000L)
                .build();

        when(receiptRepository.findAllById(any())).thenReturn(List.of(receipt));
        when(cashupRepository.findFirstByLegacyTransactionIdAndSourceOrderByCreatedAtDesc(
                "batch-eft", "ERP_ONLINE_EFT"))
                .thenReturn(Optional.of(cashup));
        when(cashupReceiptRepository.existsByCashupIdAndReceiptId("cashup-eft", "receipt-eft"))
                .thenReturn(false);
        when(cashupReceiptRepository.findByCashupId("cashup-eft"))
                .thenReturn(List.of(linkedReceipt));

        service.addReceipts(batch, List.of("receipt-eft"), "cashier-1", "device-1");

        verify(cashupRepository, never())
                .findFirstByDeviceIdAndUserIdAndStatusAndSourceOrderByCreatedAtDesc(
                        any(), any(), eq("OPEN"), eq("ERP_ONLINE"));
        verify(cashupService).submitForApproval(eq("cashup-eft"), any(CashupSubmitForApprovalRequest.class));

        ArgumentCaptor<CashupPaymentSummaryEntity> summaryCaptor =
                ArgumentCaptor.forClass(CashupPaymentSummaryEntity.class);
        verify(cashupPaymentSummaryRepository).save(summaryCaptor.capture());
        assertEquals("EFT", summaryCaptor.getValue().getPaymentMethod());
        assertEquals(20_000L, summaryCaptor.getValue().getAmountCents());
        assertEquals(1, summaryCaptor.getValue().getPaymentCount());
        assertEquals(20_000L, cashup.getTotalCents());
        assertEquals(1, cashup.getReceiptCount());
    }
}
