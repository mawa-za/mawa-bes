package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.v2.devicesync.DeviceSyncSubmitRequest;
import za.co.mawa.bes.dto.v2.devicesync.DeviceSyncCancellationRequest;
import za.co.mawa.bes.entity.DeviceSyncSubmissionEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.repository.DeviceSyncSubmissionRepository;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSyncSubmissionServiceTest {

    @Mock
    private DeviceSyncSubmissionRepository repository;

    @Mock
    private PaymentBatchRepository paymentBatchRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Test
    void completedSubmissionWithoutBackendIdentityIsNotVerified() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        DeviceSyncSubmissionEntity completed = DeviceSyncSubmissionEntity.builder()
                .submissionId("receipt-no-ack")
                .idempotencyKey("receipt:device-1:42")
                .deviceId("device-1")
                .status("COMPLETED")
                .responsePayload("{\"message\":\"ok\"}")
                .build();
        when(repository.findByIdempotencyKey("receipt:device-1:42"))
                .thenReturn(Optional.of(completed));
        var result = service.reconcile("device-1", "receipt:device-1:42");
        assertThat(result.isVerified()).isFalse();
        assertThat(result.getMessage()).contains("no verifiable backend record ID");
    }

    @Test
    void completedSubmissionWithBackendIdentityCanBeVerified() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        DeviceSyncSubmissionEntity completed = DeviceSyncSubmissionEntity.builder()
                .submissionId("receipt-ack")
                .idempotencyKey("receipt:device-1:43")
                .deviceId("device-1")
                .status("COMPLETED")
                .responsePayload("{\"id\":\"receipt-server-id\"}")
                .build();
        when(repository.findByIdempotencyKey("receipt:device-1:43"))
                .thenReturn(Optional.of(completed));
        var result = service.reconcile("device-1", "receipt:device-1:43");
        assertThat(result.isVerified()).isTrue();
        assertThat(result.getServerRecordId()).isEqualTo("receipt-server-id");
    }

    @Test
    void failedMembershipWithLostAcknowledgmentCanReconcileByVerifiedIdentity() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        DeviceSyncSubmissionEntity submitted = DeviceSyncSubmissionEntity.builder()
                .submissionId("offline-membership")
                .idempotencyKey("membership:device-1:100")
                .deviceId("device-1")
                .targetPath("/v2/membership")
                .requestPayload("{\"membershipNo\":\"M-100\",\"memberId\":\"partner-1\",\"planId\":\"plan-1\"}")
                .status("PROCESSING_FAILED")
                .build();
        when(repository.findByIdempotencyKey("membership:device-1:100"))
                .thenReturn(Optional.of(submitted));
        var membership = za.co.mawa.bes.entity.v2.MembershipEntity.builder()
                .id("erp-id").membershipNo("M-100").memberId("partner-1")
                .planId("plan-1").status("PENDING_APPROVAL").build();
        when(membershipRepository.findByMembershipNo("M-100"))
                .thenReturn(Optional.of(membership));

        var result = service.reconcile("device-1", "membership:device-1:100");
        assertThat(result.isVerified()).isTrue();
        assertThat(result.getServerRecordId()).isEqualTo("erp-id");
        assertThat(result.getServerStatus()).isEqualTo("PENDING_APPROVAL");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void conflictingMembershipIdentityIsNotReconciled() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        DeviceSyncSubmissionEntity submitted = DeviceSyncSubmissionEntity.builder()
                .submissionId("offline-membership")
                .idempotencyKey("membership:device-1:101")
                .deviceId("device-1")
                .targetPath("/v2/membership")
                .requestPayload("{\"membershipNo\":\"M-101\",\"memberId\":\"partner-1\",\"planId\":\"plan-1\"}")
                .status("PROCESSING_FAILED")
                .build();
        when(repository.findByIdempotencyKey("membership:device-1:101"))
                .thenReturn(Optional.of(submitted));
        var other = za.co.mawa.bes.entity.v2.MembershipEntity.builder()
                .id("different-erp-id").membershipNo("M-101")
                .memberId("different-partner").planId("plan-1").build();
        when(membershipRepository.findByMembershipNo("M-101"))
                .thenReturn(Optional.of(other));
        var result = service.reconcile("device-1", "membership:device-1:101");
        assertThat(result.isVerified()).isFalse();
    }

    @Test
    void manualCorrectionCannotOverwriteInFlightSubmission() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        DeviceSyncSubmissionEntity processing = DeviceSyncSubmissionEntity.builder()
                .submissionId("active-operation")
                .status("PROCESSING")
                .requestPayload("{\"original\":true}")
                .build();
        when(repository.findBySubmissionIdForUpdate("active-operation"))
                .thenReturn(Optional.of(processing));
        var correction = new za.co.mawa.bes.dto.v2.devicesync.DeviceSyncCorrectionRequest();
        correction.setPayload(Map.of("changed", true));
        assertThatThrownBy(() -> service.correct("active-operation", correction, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("being processed");
        assertThat(processing.getRequestPayload()).contains("original");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void manualCorrectionCannotReopenCompletedSubmission() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        DeviceSyncSubmissionEntity completed = DeviceSyncSubmissionEntity.builder()
                .submissionId("completed-operation")
                .status("COMPLETED")
                .build();
        when(repository.findBySubmissionIdForUpdate("completed-operation"))
                .thenReturn(Optional.of(completed));
        var correction = new za.co.mawa.bes.dto.v2.devicesync.DeviceSyncCorrectionRequest();
        correction.setPayload(Map.of("changed", true));
        assertThatThrownBy(() -> service.correct("completed-operation", correction, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("finalized");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void cancellationCannotInvalidateActiveProcessing() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        DeviceSyncSubmissionEntity processing = DeviceSyncSubmissionEntity.builder()
                .submissionId("active-payment")
                .idempotencyKey("payment-batch:device-1:42")
                .status("PROCESSING")
                .build();
        when(repository.findBySubmissionIdForUpdate("active-payment"))
                .thenReturn(Optional.of(processing));
        assertThatThrownBy(() -> service.cancel("active-payment", null, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("processing");
        assertThat(processing.getStatus()).isEqualTo("PROCESSING");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void repeatedSubmitDoesNotReplaceAnInFlightPayload() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        DeviceSyncSubmissionEntity inFlight = DeviceSyncSubmissionEntity.builder()
                .submissionId("submission-processing")
                .idempotencyKey("payment:device:123")
                .targetPath("/v2/payment-batch")
                .httpMethod("POST")
                .requestPayload("{\"reference\":\"original\"}")
                .status("PROCESSING")
                .build();
        when(repository.findByIdempotencyKeyForUpdate("payment:device:123"))
                .thenReturn(Optional.of(inFlight));
        DeviceSyncSubmitRequest retry = new DeviceSyncSubmitRequest();
        retry.setIdempotencyKey("payment:device:123");
        retry.setMethod("POST");
        retry.setPath("/v2/payment-batch");
        retry.setPayload(Map.of("reference", "replacement"));

        var result = service.submit(retry, "user-2");
        assertThat(result.getStatus()).isEqualTo("PROCESSING");
        assertThat(inFlight.getRequestPayload()).contains("original");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .save(any(DeviceSyncSubmissionEntity.class));
    }

    @Test
    void completedMembershipWithEmptyBodyIsRecoveredFromVerifiedMembershipNumber() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, mapper);
        DeviceSyncSubmissionEntity existing = DeviceSyncSubmissionEntity.builder()
                .submissionId("membership-submission")
                .idempotencyKey("membership:M-100:partner-1")
                .targetPath("/v2/membership")
                .requestPayload("{\"membershipNo\":\"M-100\",\"memberId\":\"partner-1\",\"planId\":\"plan-1\"}")
                .responseStatus(201)
                .status("COMPLETED")
                .build();
        var membership = za.co.mawa.bes.entity.v2.MembershipEntity.builder()
                .id("server-membership-id").membershipNo("M-100")
                .memberId("partner-1").planId("plan-1")
                .premiumCents(100L).startDate(java.time.LocalDate.of(2026,10,1))
                .status("ACTIVE").build();
        when(repository.findByIdempotencyKeyForUpdate("membership:M-100:partner-1"))
                .thenReturn(Optional.of(existing));
        when(membershipRepository.findByMembershipNo("M-100"))
                .thenReturn(Optional.of(membership));
        when(repository.save(any(DeviceSyncSubmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        DeviceSyncSubmitRequest request = new DeviceSyncSubmitRequest();
        request.setIdempotencyKey("membership:M-100:partner-1");
        request.setMethod("POST");
        request.setPath("/v2/membership");
        var result = service.submit(request, "user-1");
        assertThat(result.getResponseStatus()).isEqualTo(200);
        assertThat(result.getResponsePayload().toString()).contains("server-membership-id");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void emptyMembershipResponseMustNotReconcileDifferentMember() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, mapper);
        DeviceSyncSubmissionEntity existing = DeviceSyncSubmissionEntity.builder()
                .submissionId("membership-submission")
                .idempotencyKey("membership:M-100:partner-1")
                .targetPath("/v2/membership")
                .requestPayload("{\"membershipNo\":\"M-100\",\"memberId\":\"partner-1\",\"planId\":\"plan-1\"}")
                .status("COMPLETED").build();
        var unrelated = za.co.mawa.bes.entity.v2.MembershipEntity.builder()
                .id("wrong-id").membershipNo("M-100").memberId("different-partner")
                .planId("plan-1").build();
        when(repository.findByIdempotencyKeyForUpdate("membership:M-100:partner-1"))
                .thenReturn(Optional.of(existing));
        when(membershipRepository.findByMembershipNo("M-100"))
                .thenReturn(Optional.of(unrelated));
        when(repository.save(any(DeviceSyncSubmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        DeviceSyncSubmitRequest request = new DeviceSyncSubmitRequest();
        request.setIdempotencyKey("membership:M-100:partner-1");
        request.setMethod("POST");
        request.setPath("/v2/membership");
        var result = service.submit(request, "user-1");
        assertThat(result.getStatus()).isEqualTo("PROCESSING_FAILED");
        assertThat(result.getResponsePayload()).isNull();
    }

    @Test
    void retryOfFailedSubmissionRefreshesCorrectedPayloadInsteadOfReplayingStaleRequest() {
        ObjectMapper mapper = new ObjectMapper();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, mapper);

        DeviceSyncSubmissionEntity existing = DeviceSyncSubmissionEntity.builder()
                .id(7L)
                .submissionId("submission-1")
                .idempotencyKey("partner:device-1:42")
                .deviceId("device-1")
                .syncTime(LocalDateTime.now().minusMinutes(5))
                .submittedBy("user-old")
                .httpMethod("POST")
                .targetPath("/v2/partner")
                .requestPayload("{\"name1\":\"OLD\"}")
                .responsePayload("{\"message\":\"invalid\"}")
                .responseStatus(400)
                .status("CORRECTION_REQUIRED")
                .attemptCount(1)
                .errorMessage("invalid")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .updatedAt(LocalDateTime.now().minusMinutes(4))
                .processedAt(LocalDateTime.now().minusMinutes(4))
                .build();

        DeviceSyncSubmitRequest request = new DeviceSyncSubmitRequest();
        request.setIdempotencyKey("partner:device-1:42");
        request.setDeviceId("device-1");
        request.setDeviceSerialNumber("SERIAL-2");
        request.setMethod("POST");
        request.setPath("/v2/partner");
        request.setPayload(Map.of("name1", "CORRECTED"));

        when(repository.findByIdempotencyKeyForUpdate("partner:device-1:42"))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(DeviceSyncSubmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.submit(request, "user-new");

        assertThat(result.getStatus()).isEqualTo("RECEIVED");
        assertThat(existing.getRequestPayload()).contains("CORRECTED");
        assertThat(existing.getRequestPayload()).doesNotContain("OLD");
        assertThat(existing.getResponsePayload()).isNull();
        assertThat(existing.getResponseStatus()).isNull();
        assertThat(existing.getErrorMessage()).isNull();
        assertThat(existing.getProcessedAt()).isNull();
        assertThat(existing.getSubmittedBy()).isEqualTo("user-new");
        assertThat(existing.getDeviceSerialNumber()).isEqualTo("SERIAL-2");
    }

    @Test
    void completedSubmissionRemainsIdempotentAndIsNotOverwritten() {
        ObjectMapper mapper = new ObjectMapper();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, mapper);

        DeviceSyncSubmissionEntity existing = DeviceSyncSubmissionEntity.builder()
                .id(8L)
                .submissionId("submission-2")
                .idempotencyKey("partner:device-1:43")
                .deviceId("device-1")
                .syncTime(LocalDateTime.now())
                .submittedBy("user-old")
                .httpMethod("POST")
                .targetPath("/v2/partner")
                .requestPayload("{\"name1\":\"ACCEPTED\"}")
                .responseStatus(200)
                .status("COMPLETED")
                .attemptCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DeviceSyncSubmitRequest request = new DeviceSyncSubmitRequest();
        request.setIdempotencyKey("partner:device-1:43");
        request.setDeviceId("device-1");
        request.setMethod("POST");
        request.setPath("/v2/partner");
        request.setPayload(Map.of("name1", "SHOULD-NOT-REPLACE"));

        when(repository.findByIdempotencyKeyForUpdate("partner:device-1:43"))
                .thenReturn(Optional.of(existing));

        var result = service.submit(request, "user-new");

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(existing.getRequestPayload()).contains("ACCEPTED");
        assertThat(existing.getRequestPayload()).doesNotContain("SHOULD-NOT-REPLACE");
    }

    @Test
    void completedPaymentWithoutResponseBodyIsReopenedForARealRetry() {
        ObjectMapper mapper = new ObjectMapper();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, mapper);
        DeviceSyncSubmissionEntity existing = DeviceSyncSubmissionEntity.builder()
                .id(10L)
                .submissionId("submission-empty-payment")
                .idempotencyKey("payment-batch-number:device-1:PB-9002")
                .deviceId("device-1")
                .syncTime(LocalDateTime.now())
                .submittedBy("cashier")
                .httpMethod("POST")
                .targetPath("/v2/sync/payment-batches/membership-premiums")
                .requestPayload("{\"paymentBatchNo\":\"PB-9002\"}")
                .responsePayload(null)
                .responseStatus(200)
                .status("COMPLETED")
                .attemptCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        DeviceSyncSubmitRequest request = new DeviceSyncSubmitRequest();
        request.setIdempotencyKey("payment-batch-number:device-1:PB-9002");
        request.setDeviceId("device-1");
        request.setMethod("POST");
        request.setPath("/v2/sync/payment-batches/membership-premiums");
        request.setPayload(Map.of("paymentBatchNo", "PB-9002"));

        when(repository.findByIdempotencyKeyForUpdate(request.getIdempotencyKey()))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(DeviceSyncSubmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.submit(request, "cashier");

        assertThat(result.getStatus()).isEqualTo("RECEIVED");
        assertThat(existing.getResponseStatus()).isNull();
        assertThat(existing.getProcessedAt()).isNull();
    }

    @Test
    void orphanedPaymentSubmissionCanBeCancelledWithoutDeletingAuditHistory() {
        ObjectMapper mapper = new ObjectMapper();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, mapper);
        DeviceSyncSubmissionEntity existing = DeviceSyncSubmissionEntity.builder()
                .id(9L)
                .submissionId("submission-3")
                .idempotencyKey("payment-batch:device-1:44")
                .deviceId("device-1")
                .syncTime(LocalDateTime.now())
                .submittedBy("cashier")
                .httpMethod("POST")
                .targetPath("/v2/sync/payment-batches/membership-premiums")
                .requestPayload("{\"deviceId\":\"device-1\",\"localPaymentBatchId\":\"44\"}")
                .status("CORRECTION_REQUIRED")
                .attemptCount(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        DeviceSyncCancellationRequest request = new DeviceSyncCancellationRequest();
        request.setDeviceId("device-1");
        request.setReason("Local receipt deleted before successful synchronization");

        when(repository.findBySubmissionId("submission-3")).thenReturn(Optional.of(existing));
        when(paymentBatchRepository.findByDeviceIdAndLocalPaymentBatchId("device-1", "44"))
                .thenReturn(Optional.empty());
        when(repository.save(any(DeviceSyncSubmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.cancel("submission-3", request, "cashier");

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(existing.getErrorMessage()).contains("cashier");
        assertThat(existing.getProcessedAt()).isNotNull();
    }

    @Test
    void paymentBatchNumberIdentityDoesNotResolveAReusedLocalRowId() {
        ObjectMapper mapper = new ObjectMapper();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, mapper);
        PaymentBatchEntity current = PaymentBatchEntity.builder()
                .id("server-batch-new")
                .paymentBatchNo("PB-9002")
                .deviceId("device-1")
                .localPaymentBatchId("44")
                .status(PaymentBatchStatus.POSTED)
                .build();

        when(repository.findByIdempotencyKeyForUpdate(
                "payment-batch-number:device-1:PB-9002"))
                .thenReturn(Optional.empty());
        when(paymentBatchRepository.findByPaymentBatchNo("PB-9002"))
                .thenReturn(Optional.of(current));
        when(receiptRepository.findByPaymentBatchId("server-batch-new"))
                .thenReturn(java.util.List.of());

        var result = service.reconcile(
                "device-1", "payment-batch-number:device-1:PB-9002");

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getServerRecordId()).isEqualTo("server-batch-new");
    }

    @Test
    void reconciliationRejectsDuplicateBackendReceiptNumbers() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        PaymentBatchEntity batch = PaymentBatchEntity.builder()
                .id("server-batch-1").paymentBatchNo("PB-100")
                .deviceId("device-1").status(PaymentBatchStatus.POSTED).build();
        when(repository.findByIdempotencyKeyForUpdate("payment-batch-number:device-1:PB-100"))
                .thenReturn(Optional.empty());
        when(paymentBatchRepository.findByPaymentBatchNo("PB-100"))
                .thenReturn(Optional.of(batch));
        when(receiptRepository.findByPaymentBatchId("server-batch-1"))
                .thenReturn(java.util.List.of(
                        za.co.mawa.bes.entity.v2.ReceiptEntity.builder()
                                .id("receipt-1").receiptNo("R-100").build(),
                        za.co.mawa.bes.entity.v2.ReceiptEntity.builder()
                                .id("receipt-2").receiptNo("R-100").build()));

        var result = service.reconcile("device-1", "payment-batch-number:device-1:PB-100");
        assertThat(result.isVerified()).isFalse();
        assertThat(result.getMessage()).contains("duplicate");
    }

    @Test
    void reconciliationRejectsReceiptWithoutServerId() {
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(
                repository, paymentBatchRepository, receiptRepository, membershipRepository, new ObjectMapper());
        PaymentBatchEntity batch = PaymentBatchEntity.builder()
                .id("server-batch-2").paymentBatchNo("PB-101")
                .deviceId("device-1").status(PaymentBatchStatus.POSTED).build();
        when(repository.findByIdempotencyKeyForUpdate("payment-batch-number:device-1:PB-101"))
                .thenReturn(Optional.empty());
        when(paymentBatchRepository.findByPaymentBatchNo("PB-101"))
                .thenReturn(Optional.of(batch));
        when(receiptRepository.findByPaymentBatchId("server-batch-2"))
                .thenReturn(java.util.List.of(
                        za.co.mawa.bes.entity.v2.ReceiptEntity.builder()
                                .id(null).receiptNo("R-101").build()));
        var result = service.reconcile("device-1", "payment-batch-number:device-1:PB-101");
        assertThat(result.isVerified()).isFalse();
    }
}
