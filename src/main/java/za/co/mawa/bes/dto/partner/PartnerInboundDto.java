package za.co.mawa.bes.dto.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class PartnerInboundDto {
    private String identityType;
    private String identityNumber;
    private String partnerType;
    private String name1;
    private String name2;
    private String name3;
    private String name4;
    private String title;
    private Date birthDate;
    private String maritalStatus;
    private String gender;
    private String language;

}
