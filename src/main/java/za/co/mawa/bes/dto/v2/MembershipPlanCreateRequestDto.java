package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanCreateRequestDto {

    private String planCode;
    private String name;
    private String description;
    private Long premiumCents;
    private String currency;
    private Integer maxDependents;
    private Boolean active;
    private String oldId;
}
