package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PremiumPaymentDeletionRequest {
    private String requesterId;
    private String reason;
}
