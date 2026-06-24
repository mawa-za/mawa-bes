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
public class MembershipDependentCreateRequestDto {

    private String membershipId;
    private String dependentPartnerId;
    private DependentType dependentType;
    private Boolean active;
}
