package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipClaimLinkUpdateRequestDto {

    private String id;
    private String parentClaimId;
    private String linkedClaimId;
}
