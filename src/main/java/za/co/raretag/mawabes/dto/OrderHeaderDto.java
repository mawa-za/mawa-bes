package za.co.raretag.mawabes.dto;

public class OrderHeaderDto {

    private String id;
    private String type;
    private String subType;
    private String description;
    private String subDescription;
    private String status;
    private String statusReason;
    private String location;
    private String subStatus;
    private String validFrom;
    private String validTo;
    private String createdBy;
    private String changedBy;

//    public OrderHeaderDto(String id, String type, String subType, String description,
//                          String subDescription, String status, String statusReason, String location,
//                          String subStatus, String validFrom, String validTo, String createdBy, String changedBy) {
//        this.id = id;
//        this.type = type;
//        this.subType = subType;
//        this.description = description;
//        this.subDescription = subDescription;
//        this.status = status;
//        this.statusReason = statusReason;
//        this.location = location;
//        this.subStatus = subStatus;
//        this.validFrom = validFrom;
//        this.validTo = validTo;
//        this.createdBy = createdBy;
//        this.changedBy = changedBy;
//    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubDescription() {
        return this.subDescription;
    }

    public void setSubDescription(String subDescription) {
        this.subDescription = subDescription;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSubStatus() {
        return subStatus;
    }

    public void setSubStatus(String subStatus) {
        this.subStatus = subStatus;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
