package za.co.mawa.bes.dto.v2;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MembershipPartnerIdentityCorrectionRequest {
    @NotBlank private String subjectType;
    private String dependentId;
    @NotBlank private String identityNumber;
    @NotBlank private String reason;
    private Boolean overrideExistingOwner = false;
}
