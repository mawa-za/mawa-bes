package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import za.co.mawa.bes.entity.DeviceSyncSubmissionEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.repository.DeviceSyncSubmissionRepository;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSyncSubmissionFirstAttemptTest {
    @Mock DeviceSyncSubmissionRepository repository;
    @Mock PaymentBatchRepository paymentBatchRepository;
    @Mock ReceiptRepository receiptRepository;
    @Mock MembershipRepository membershipRepository;

    @Test
    void aNewQueueKeyDoesNotRepostAnExistingDevicePaymentBatch() {
        var service = new DeviceSyncSubmissionService(repository, paymentBatchRepository,
                receiptRepository, membershipRepository, new ObjectMapper());
        var queued = DeviceSyncSubmissionEntity.builder()
                .submissionId("new-key")
                .idempotencyKey("different-operation-key")
                .deviceId("device-1")
                .targetPath("/v2/sync/payment-batches/create")
                .requestPayload("{\"deviceId\":\"device-1\",\"paymentBatchNo\":\"B100\",\"localPaymentBatchId\":\"10\"}")
                .status("RECEIVED")
                .attemptCount(0)
                .build();
        when(repository.findBySubmissionIdForUpdate("new-key")).thenReturn(Optional.of(queued));
        when(paymentBatchRepository.findByPaymentBatchNo("B100"))
                .thenReturn(Optional.of(PaymentBatchEntity.builder()
                        .id("erp-existing").deviceId("device-1").build()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.process("new-key", new HttpHeaders());
        assertThat(result.getStatus()).isEqualTo("PROCESSING_FAILED");
        assertThat(queued.getErrorMessage()).contains("already exists in ERP");
        assertThat(queued.getAttemptCount()).isZero();
    }
}
