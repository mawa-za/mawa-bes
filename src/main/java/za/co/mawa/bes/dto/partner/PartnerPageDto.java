package za.co.mawa.bes.dto.partner;

import java.io.Serializable;
import java.util.Date;

public class PartnerPageDto implements Serializable {

//    private static final long serialVersionUID = 1L;

    private String type;
    private String id;
    private String number;
    private String idType;
    private String idNumber;
    private String name1;
    private String name2;
    private String name3;
    private String title;
    private Date birthDate;
    private String maritalStatus;
    private String gender;
    private String language;
    private String status;
    private String statusReason;
    private String createdBy;
    private Date validFrom;
    private Date validTo;
    private Date creationDate;

    // Full Constructor
    public PartnerPageDto(String type, String id, String number, String idType, String idNumber,
                          String name1, String name2, String name3, String title, Date birthDate,
                          String maritalStatus, String gender, String language, String status,
                          String statusReason, String createdBy, Date validFrom, Date validTo,
                          Date creationDate) {
        this.type = type;
        this.id = id;
        this.number = number;
        this.idType = idType;
        this.idNumber = idNumber;
        this.name1 = name1;
        this.name2 = name2;
        this.name3 = name3;
        this.title = title;
        this.birthDate = birthDate;
        this.maritalStatus = maritalStatus;
        this.gender = gender;
        this.language = language;
        this.status = status;
        this.statusReason = statusReason;
        this.createdBy = createdBy;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.creationDate = creationDate;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public String getName3() {
        return name3;
    }

    public void setName3(String name3) {
        this.name3 = name3;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

}

