package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.PaymentBatchStatus;

@Getter
@Builder
public class PremiumPaymentDeletionStatusResponse {
    private String paymentBatchId;
    private PaymentBatchStatus paymentBatchStatus;
    private String approvalRequestId;
    private ApprovalStatus approvalStatus;
}
