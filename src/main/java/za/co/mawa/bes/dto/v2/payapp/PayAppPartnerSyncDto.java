package za.co.mawa.bes.dto.v2.payapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayAppPartnerSyncDto {
    private String partnerId;
    private String partnerNo;
    private String partnerType;
    private String firstName;
    private String lastName;
    private String middleName;
    private String identityType;
    private String identityNumber;
    private String partnerStatus;
    private LocalDate birthDate;
    private String gender;
    private String email;
    private String mobileNumber;
}
