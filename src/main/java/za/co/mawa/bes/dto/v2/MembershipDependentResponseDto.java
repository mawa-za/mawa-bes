package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipDependentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipDependentResponseDto {

    private String id;
    private String membershipId;
    private String dependentPartnerId;
    private String firstName;
    private String lastName;
    private String number;
    private String identityType;
    private String identityNumber;
    private DependentType dependentType;
    private Boolean active;
    private MembershipDependentStatus status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private LocalDate deceasedDate;
    private String statusReason;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
