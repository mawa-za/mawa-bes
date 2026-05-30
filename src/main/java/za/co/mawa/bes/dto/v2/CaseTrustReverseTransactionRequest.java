package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseTrustReverseTransactionRequest {

    private String reason;
    private String reversedBy;
}
