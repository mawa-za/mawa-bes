package za.co.mawa.bes.dto.v2.membership.change;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.mawa.bes.enums.DependentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipDependentAddRequest {
    private String dependentPartnerId;
    private DependentType dependentType;
    private String reason;
}
