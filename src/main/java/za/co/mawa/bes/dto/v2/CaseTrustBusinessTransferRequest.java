package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseTrustBusinessTransferRequest {

    private String invoiceId;
    private Long amountCents;
    private String description;
    private String transferredBy;
}
