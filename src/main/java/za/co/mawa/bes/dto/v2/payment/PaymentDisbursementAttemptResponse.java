package za.co.mawa.bes.dto.v2.payment;

import lombok.Builder;
import lombok.Value;
import za.co.mawa.bes.enums.PaymentDisbursementAttemptStatus;

import java.time.LocalDateTime;

@Value
@Builder
public class PaymentDisbursementAttemptResponse {
    String id;
    String paymentRequestId;
    Integer attemptNo;
    String provider;
    PaymentDisbursementAttemptStatus status;
    String instructionId;
    String providerStatus;
    String failureCode;
    String failureMessage;
    boolean bankReportAvailable;
    LocalDateTime bankReportRetrievedAt;
    LocalDateTime submittedAt;
    LocalDateTime lastCheckedAt;
    LocalDateTime completedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
