package za.co.mawa.bes.dto.v2;

import lombok.Data;

@Data
public class PremiumPaymentEditRequest {
    private String receiptId;
    private Long amountCents;
    private String periodYYYYMM;
    private String requestedBy;
    private String reason;
}
