package za.co.mawa.bes.service.v2;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.entity.v2.PaymentRequestStatusHistoryEntity;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.v2.BankPaymentService;
import za.co.mawa.bes.repository.v2.PaymentRequestRepository;
import za.co.mawa.bes.repository.v2.PaymentRequestStatusHistoryRepository;
import za.co.mawa.bes.service.MessageProducerService;
import za.co.mawa.bes.service.SettingService;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRequestFnbPaymentQueueServiceTest {

    @Mock private PaymentRequestRepository paymentRequestRepository;
    @Mock private PaymentRequestStatusHistoryRepository statusHistoryRepository;
    @Mock private MessageProducerService messageProducerService;
    @Mock private SettingService settingService;
    @Mock private Gson gson;
    @Mock private PaymentDisbursementAttemptService attemptService;
    @Mock private PaymentAccountConfigurationService paymentAccountConfigurationService;
    @Mock private BankPaymentService bankPaymentService;

    private PaymentRequestFnbPaymentQueueService service;

    @BeforeEach
    void setUp() {
        service = new PaymentRequestFnbPaymentQueueService(
                paymentRequestRepository,
                statusHistoryRepository,
                messageProducerService,
                settingService,
                gson,
                attemptService,
                paymentAccountConfigurationService
        );
        ReflectionTestUtils.setField(service, "bankPaymentService", bankPaymentService);
    }

    @Test
    void refreshesClaimGeneratedManualRoutingAndQueuesWhenFnbDebtorNowExists() {
        PaymentRequestEntity paymentRequest = approvedFuneralPayment();
        when(paymentRequestRepository.findById("payment-1")).thenReturn(Optional.of(paymentRequest));
        when(settingService.getSetting("ENABLED", "FNB-API")).thenReturn("true");
        when(paymentAccountConfigurationService.activeDebtor("FUNERAL_SERVICE_PAYMENT"))
                .thenReturn(Optional.of(Map.of(
                        "id", "debtor-1",
                        "bank_integration", "FNB"
                )));
        when(bankPaymentService.generateRequest(paymentRequest)).thenReturn(new BankPaymentRequest());
        when(gson.toJson(any(BankPaymentRequest.class))).thenReturn("{}");
        when(paymentRequestRepository.saveAndFlush(paymentRequest)).thenReturn(paymentRequest);
        when(paymentRequestRepository.save(paymentRequest)).thenReturn(paymentRequest);

        service.queueAfterApproval("payment-1", "PR-0001", "approver-1");

        assertEquals(PaymentMethod.EFT, paymentRequest.getPaymentMethod());
        assertEquals("debtor-1", paymentRequest.getDebtorAccountId());
        assertEquals("FNB", paymentRequest.getBankIntegration());
        assertEquals(PaymentRequestStatus.QUEUED_FOR_PAYMENT, paymentRequest.getStatus());
        verify(messageProducerService).sendMessageIfNotExists(any());
        verify(attemptService).ensureQueued("payment-1");
    }

    @Test
    void automaticClaimApprovalKeepsApprovedRequestAndRecordsReasonWhenDebtorMappingIsMissing() {
        PaymentRequestEntity paymentRequest = approvedFuneralPayment();
        when(paymentRequestRepository.findById("payment-1")).thenReturn(Optional.of(paymentRequest));
        when(settingService.getSetting("ENABLED", "FNB-API")).thenReturn("true");
        when(paymentAccountConfigurationService.activeDebtor("FUNERAL_SERVICE_PAYMENT"))
                .thenReturn(Optional.empty());

        service.queueAfterApproval("payment-1", "PR-0001", "approver-1");

        assertEquals(PaymentRequestStatus.APPROVED, paymentRequest.getStatus());
        verify(messageProducerService, never()).sendMessageIfNotExists(any());
        ArgumentCaptor<PaymentRequestStatusHistoryEntity> historyCaptor =
                ArgumentCaptor.forClass(PaymentRequestStatusHistoryEntity.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertEquals(
                "Bank message not queued because no active debtor account is configured for payment request type FUNERAL_SERVICE_PAYMENT.",
                historyCaptor.getValue().getComment()
        );
    }

    @Test
    void explicitRetryReturnsConfigurationErrorWhenDebtorMappingIsMissing() {
        PaymentRequestEntity paymentRequest = approvedFuneralPayment();
        when(paymentRequestRepository.findById("payment-1")).thenReturn(Optional.of(paymentRequest));
        when(settingService.getSetting("ENABLED", "FNB-API")).thenReturn("true");
        when(paymentAccountConfigurationService.activeDebtor("FUNERAL_SERVICE_PAYMENT"))
                .thenReturn(Optional.empty());

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.queueForBank("payment-1", "PR-0001", "approver-1")
        );

        assertEquals(
                "Bank message not queued because no active debtor account is configured for payment request type FUNERAL_SERVICE_PAYMENT.",
                error.getMessage()
        );
        verify(messageProducerService, never()).sendMessageIfNotExists(any());
    }

    private PaymentRequestEntity approvedFuneralPayment() {
        PaymentRequestEntity paymentRequest = new PaymentRequestEntity();
        paymentRequest.setId("payment-1");
        paymentRequest.setRequestNo("PR-0001");
        paymentRequest.setRequestType(PaymentRequestType.FUNERAL_SERVICE_PAYMENT);
        paymentRequest.setStatus(PaymentRequestStatus.APPROVED);
        paymentRequest.setPaymentMethod(PaymentMethod.MANUAL);
        return paymentRequest;
    }
}
