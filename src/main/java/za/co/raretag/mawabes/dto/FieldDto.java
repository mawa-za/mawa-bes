package za.co.raretag.mawabes.dto;

import za.co.raretag.mawabes.entity.UserEntity;

public class FieldDto {
    private String id;
    private String status;
    private String statusReason;
    private String code;
    private String description;
    private String validFrom;
    private String validTo;
    private PersonDto createdBy;
    private String ownedBy;
    private UserEntity user;

    public FieldDto(String id, String status, String statusReason, String code,
                    String description, String validFrom, String validTo,
                    PersonDto createdBy, String ownedBy, UserEntity user) {
        this.id = id;
        this.status = status;
        this.statusReason = statusReason;
        this.code = code;
        this.description = description;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdBy = createdBy;
        this.ownedBy = ownedBy;
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public PersonDto getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(PersonDto createdBy) {
        this.createdBy = createdBy;
    }

    public String getOwnedBy() {
        return ownedBy;
    }

    public void setOwnedBy(String ownedBy) {
        this.ownedBy = ownedBy;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}
