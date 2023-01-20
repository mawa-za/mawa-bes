package za.co.mawa.bes.dto;

public class PersonDto {

    public PersonDto() {
    }

    public PersonDto(PartnerDto partner) {

        if (partner.getId() != null) {
            this.id = partner.getId();
        }
        if (partner.getIdType() != null) {
            this.idType = partner.getIdType();
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


    }

    private String id;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    private String idType;
    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }
    private String idNumber;
    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
    private String type;
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    private String fullName;
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    private String middleName;
    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }
    private String firstName;
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    private String gender;
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
    private String birthDate;
    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
    private String language;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }



    private String maritalStatus;
    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }
    private String title;
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    private String status;
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    private String statusReason;

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
    private String createdBy;
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    private String validFrom;
    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }
    private String validTo;
    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }
}
