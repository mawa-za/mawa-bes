package za.co.mawa.bes.dto.v2.payapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayAppMemberSyncDto {
    private String membershipId;
    private String membershipNo;
    private String partnerId;
    private String partnerNo;
    private String firstName;
    private String lastName;
    private String middleName;
    private String identityType;
    private String identityNumber;
    private String partnerStatus;
    private LocalDate birthDate;
    private String gender;
    private String planId;
    private String membershipStatus;
    private String paidUpToPeriod;
    private LocalDate startDate;
    private LocalDate joinDate;
    private LocalDateTime updatedAt;
}
