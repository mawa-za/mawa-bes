package za.co.mawa.bes.dto.v2.membership.lapse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipLapseRunResultDto {
    private boolean skipped;
    private String reason;
    private int threshold;
    private int evaluatedMemberships;
    private int membershipsWithOverduePremiums;
    private int lapsedMemberships;
    private String runDate;
    @Builder.Default
    private List<String> lapsedMembershipIds = new ArrayList<>();
}
