package za.co.mawa.bes.dto.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.IdentityDto;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class PartnerDto {

    private FieldOptionDto type;
    private String id;
    private String number;
    private PartnerIdentityDto identity;
    private String name1;
    private String name2;
    private String name3;
    private String name4;
    private FieldOptionDto title;
    private Date birthDate;
    private FieldOptionDto maritalStatus;
    private FieldOptionDto gender;
    private FieldOptionDto language;
    private FieldOptionDto status;
    private String createdBy;
    private String modifiedBy;
    private Date validFrom;
    private Date validTo;


}
