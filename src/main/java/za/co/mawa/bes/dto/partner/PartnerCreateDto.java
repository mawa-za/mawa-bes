package za.co.mawa.bes.dto.partner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class PartnerCreateDto {
    private String type;
    private String name1;
    private String name2;
    private String name3;
    private String name4;
    private String title;
    private Date birthDate;
    private String maritalStatus;
    private String gender;
    private String language;
    private String status;
    private String createdBy;
    private String modifiedBy;
    private Date validFrom;
    private Date validTo;

}
