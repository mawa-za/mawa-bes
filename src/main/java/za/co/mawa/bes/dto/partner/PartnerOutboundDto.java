package za.co.mawa.bes.dto.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class PartnerOutboundDto implements Serializable {

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
