package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PersonDto {
    private String id;
    private String number;
    private String idType;

    private String idNumber;

    private String type;

    private String fullName;

    private String lastName;


    private String middleName;

    private String firstName;

    private String gender;

    private String birthDate;

    private String language;

    private String maritalStatus;

    private String title;

    private String status;

    private String statusReason;

    private String createdBy;

    private String validFrom;

    private String validTo;

}
