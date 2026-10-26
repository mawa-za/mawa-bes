package za.co.mawa.bes.dto.v2.membership.change;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MembershipDependentDeceasedRequest {
    private LocalDate deceasedDate;
    private String reason;
}
