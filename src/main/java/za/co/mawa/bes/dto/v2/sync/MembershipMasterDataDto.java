package za.co.mawa.bes.dto.v2.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipMasterDataDto {
    private String membershipId;
    private String membershipNo;
    private String partnerId;
    private String planId;
    private Long premiumCents;
    private LocalDate startDate;
    private LocalDate joinDate;
    private String membershipStatus;
    private String paidUpToPeriod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String partnerNo;
    private String partnerType;
    private String name1;
    private String name2;
    private String name3;
    private String identityType;
    private String identityNumber;
    private Date birthDate;
    private String gender;
    private String partnerStatus;
}
