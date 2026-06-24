package za.co.mawa.bes.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerViewUpdateRequestDto {

    private String partnerId;
    private String partnerNo;
    private String partnerType;
    private String partnerRole;
    private String identityType;
    private String identityNumber;
    private String name1;
    private String name2;
    private String name3;
    private Date birthDate;
    private String gender;
    private String title;
    private String status;
    private String maritalStatus;
}
