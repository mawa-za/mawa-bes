package za.co.mawa.bes.dto.v2;

import lombok.Data;

@Data
public class PremiumPaymentTransferRequest {
    private String targetMembershipId;
    private String targetPeriodYYYYMM;
    private String requestedBy;
    private String reason;
}
