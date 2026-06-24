package za.co.mawa.bes.dto.v2;

import za.co.mawa.bes.enums.DependentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipDependentUpdateRequestDto {

    private String id;
    private String membershipId;
    private String dependentPartnerId;
    private DependentType dependentType;
    private Boolean active;
}
