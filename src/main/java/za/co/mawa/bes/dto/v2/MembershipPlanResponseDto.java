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
public class MembershipPlanResponseDto {

    private String id;
    private String planCode;
    private String name;
    private String description;
    private Long premiumCents;
    private String currency;
    private Integer maxDependents;
    private Boolean active;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String oldId;
}
