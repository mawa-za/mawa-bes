package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanPremiumRuleResponseDto {

    private String id;
    private String plan;
    private String dependentTypeId;
    private String minAgeId;
    private String maxAgeId;
    private Long additionalPremiumCents;
    private Boolean active;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
