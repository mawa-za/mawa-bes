package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.TrustPaymentMethod;

@Getter
@Setter
public class CaseTrustRefundRequest {

    private Long amountCents;
    private TrustPaymentMethod paymentMethod;
    private String referenceNo;
    private String bankReference;
    private String description;
    private String refundedBy;
}
