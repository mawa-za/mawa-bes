package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CaseDisbursementType;
import za.co.mawa.bes.enums.TrustPaymentMethod;

@Getter
@Setter
public class CaseTrustThirdPartyPaymentRequest {

    private Long amountCents;
    private String payeeName;
    private TrustPaymentMethod paymentMethod;
    private String referenceNo;
    private String bankReference;
    private String description;
    private Boolean createDisbursement;
    private CaseDisbursementType disbursementType;
    private Boolean billableDisbursement;
    private String paidBy;
}
