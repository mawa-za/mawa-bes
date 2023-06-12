package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PersonDto {
    public PersonDto(PartnerDto partner) {
        if(partner.getNumber() != null)
        {
            this.number = partner.getNumber();
        }
        if (partner.getId() != null) {
            this.id = partner.getId();
        }
        if (partner.getIdType() != null) {
            this.idType = partner.getIdType();
        }
        if(partner.getType() != null)
        {
            this.type = partner.getType();
        }
        if (partner.getIdNumber() != null) {
            this.idNumber = partner.getIdNumber();
        }
        if (partner.getName1() != null) {
            this.lastName = partner.getName1();
        }
        if (partner.getName3() != null) {
            this.middleName = partner.getName3();
        }
        if (partner.getName2() != null) {
            this.firstName = partner.getName2();
        }
        if (partner.getGender() != null) {
            this.gender = partner.getGender();
        }
        if (partner.getBirthDate() != null) {
            this.birthDate = partner.getBirthDate();
        }
        if (partner.getLanguage() != null) {
            this.language = partner.getLanguage();
        }
        if (partner.getMaritalStatus() != null) {
            this.maritalStatus = partner.getMaritalStatus();
        }
        if (partner.getTitle() != null) {
            this.title = partner.getTitle();
        }
        if (partner.getStatus() != null) {
            this.status = partner.getStatus();
        }
        if (partner.getValidFrom() != null) {
            this.validFrom = partner.getValidFrom();
        }
        if (partner.getValidTo() != null) {
            this.validTo = partner.getValidTo();
        }

        if (partner.getIdNumber() != null)
        {
            this.idNumber = partner.getIdNumber();
        }

    }

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
