package za.co.mawa.bes.dto;

import java.io.Serializable;

public class PartnerAttachmentDto implements Serializable {

    private String id;
    private String type;
    private String parent;
    private String fileName;
    private String extension;
    private String createdBy;
    private String createdAt;
    private String attachedById;
    private PersonDto attachedBy;
    private String status;
    private String statusReason;
    private String validFrom;
    private String validTo;

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

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getAttachedById() {
        return attachedById;
    }

    public void setAttachedById(String attachedById) {
        this.attachedById = attachedById;
    }

    public PersonDto getAttachedBy() {
        return attachedBy;
    }

    public void setAttachedBy(PersonDto attachedBy) {
        this.attachedBy = attachedBy;
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
}
