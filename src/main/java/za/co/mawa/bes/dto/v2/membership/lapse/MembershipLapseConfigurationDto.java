package za.co.mawa.bes.dto.v2.membership.lapse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipLapseConfigurationDto {
    private String id;
    private boolean enabled;
    private int missedPremiumsBeforeLapse;
    private String lastRunAt;
    private int lastLapsedCount;
    private String updatedAt;
    private String updatedBy;
}
