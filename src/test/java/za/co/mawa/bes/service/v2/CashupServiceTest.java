package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.payapp.CashupRequest;
import za.co.mawa.bes.dto.v2.payapp.CashupResponse;
import za.co.mawa.bes.dto.v2.payapp.CashupSubmitForApprovalRequest;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.repository.v2.CashupDepositRepository;
import za.co.mawa.bes.repository.v2.CashupPaymentSummaryRepository;
import za.co.mawa.bes.repository.v2.CashupReceiptRepository;
import za.co.mawa.bes.repository.v2.CashupRepository;
import za.co.mawa.bes.repository.v2.ManualPremiumReceiptRepository;
import za.co.mawa.bes.service.AttachmentService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashupServiceTest {

    @Mock private CashupRepository cashupRepository;
    @Mock private CashupPaymentSummaryRepository cashupPaymentSummaryRepository;
    @Mock private CashupReceiptRepository cashupReceiptRepository;
    @Mock private CashupDepositRepository cashupDepositRepository;
    @Mock private ManualPremiumReceiptRepository manualPremiumReceiptRepository;
    @Mock private UserRepository userRepository;
    @Mock private PartnerRepository partnerRepository;
    @Mock private AttachmentService attachmentService;
    @Mock private NumberAllocationService numberAllocationService;
    @Mock private ApprovalService approvalService;
    @Mock private ReferenceDataValidationService referenceDataValidationService;
    @Mock private ManualReceiptBookService manualReceiptBookService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private CashupService service;

    @Test
    void submitForApprovalSerialisesLocalDateWithoutReflectiveAccessFailure() {
        CashupEntity cashup = CashupEntity.builder()
                .id("cashup-1")
                .cashupNo(1001L)
                .deviceId("device-1")
                .userId("user-1")
                .cashupDate(LocalDate.of(2026, 8, 3))
                .totalCents(12_500L)
                .receiptCount(2)
                .status("AWAITING_DEPOSITS")
                .depositTotalCents(0L)
                .depositCount(0)
                .source("MAWAPAY")
                .build();

        when(cashupRepository.findById("cashup-1")).thenReturn(Optional.of(cashup));
        when(cashupDepositRepository.findByCashupIdOrderByDepositDateDescCreatedAtDesc("cashup-1"))
                .thenReturn(List.of());
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());
        when(approvalService.submitForApproval(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ApprovalRequestResponse.builder().id("approval-1").build());

        CashupSubmitForApprovalRequest request = new CashupSubmitForApprovalRequest();
        request.setRequesterId("requester-1");

        CashupResponse response = service.submitForApproval("cashup-1", request);

        ArgumentCaptor<ApprovalSubmitRequest> approvalCaptor = ArgumentCaptor.forClass(ApprovalSubmitRequest.class);
        verify(approvalService).submitForApproval(approvalCaptor.capture());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("approval-1", response.getApprovalRequestId());
        assertTrue(approvalCaptor.getValue().getPayloadJson().contains("\"cashupDate\":\"2026-08-03\""));
    }
    @Test
    void mawaPayCardCashupRemainsDepositRequiredEvenWithEftMarker() {
        CashupRequest request = new CashupRequest();
        request.setCashupNo(2001L);
        request.setDeviceId("device-card");
        request.setUserId("cashier-card");
        request.setDate("2026-08-22");
        request.setStatus("SUBMITTED");
        request.setTotalCents(15_000L);
        request.setReceiptCount(1);
        request.setAmountByMethod(Map.of("CARD", 15_000L));
        request.setCountByMethod(Map.of("CARD", 1));
        request.setNotes("SOURCE: MAWA_PAY_EFT; CARD payment should still require a deposit");

        when(cashupRepository.findByCashupNo(2001L)).thenReturn(Optional.empty());
        when(cashupRepository.save(any(CashupEntity.class))).thenAnswer(invocation -> {
            CashupEntity saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId("cashup-card");
            return saved;
        });
        when(cashupPaymentSummaryRepository.findByCashupId("cashup-card")).thenReturn(List.of());
        when(cashupReceiptRepository.findByCashupId("cashup-card")).thenReturn(List.of());

        CashupResponse response = service.submitCashup(request);

        ArgumentCaptor<CashupEntity> cashupCaptor = ArgumentCaptor.forClass(CashupEntity.class);
        verify(cashupRepository).save(cashupCaptor.capture());
        CashupEntity saved = cashupCaptor.getValue();

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("AWAITING_DEPOSITS", saved.getStatus());
        assertNotEquals("MAWA_PAY_EFT", saved.getSource());
        verify(approvalService, never()).submitForApproval(any());
    }

}
