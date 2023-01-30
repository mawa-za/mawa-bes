package za.co.mawa.bes.dto;

import java.io.Serializable;

public class PartnerResourceApiResultDto implements Serializable {
    private String resourceID;
    private String partnerID;
    private String partnerUrl;
    private String validFrom;
    private String validTo;
    private String status;
    private String statusReason;

    public String getResourceID() {
        return resourceID;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public String getPartnerID() {
        return partnerID;
    }

    public void setPartnerID(String partnerID) {
        this.partnerID = partnerID;
    }

    public String getPartnerUrl() {
        return partnerUrl;
    }

    public void setPartnerUrl(String partnerUrl) {
        this.partnerUrl = partnerUrl;
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
}
