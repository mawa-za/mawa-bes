package za.co.mawa.bes.dto.v2.membership.change;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipDependentRemoveRequest {
    private String reason;
}
