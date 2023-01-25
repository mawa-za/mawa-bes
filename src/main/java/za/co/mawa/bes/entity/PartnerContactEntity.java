package za.co.mawa.bes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.io.Serializable;
import java.util.Date;

public class PartnerContactEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerContactPKEntity partnerContactPK;
    @Column(name = "value", length = 60)
    private String value;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    public PartnerContactEntity() {
    }

    public PartnerContactEntity(PartnerContactPKEntity partnerContactPK) {
        this.partnerContactPK = partnerContactPK;
    }

    public PartnerContactEntity(String partner, String type) {
        this.partnerContactPK = new PartnerContactPKEntity(partner, type);
    }

    public PartnerContactPKEntity getPartnerContactPK() {
        return partnerContactPK;
    }

    public void setPartnerContactPK(PartnerContactPKEntity partnerContactPK) {
        this.partnerContactPK = partnerContactPK;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (partnerContactPK != null ? partnerContactPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerContactEntity)) {
            return false;
        }
        PartnerContactEntity other = (PartnerContactEntity) object;
        if ((this.partnerContactPK == null && other.partnerContactPK != null) || (this.partnerContactPK != null && !this.partnerContactPK.equals(other.partnerContactPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerContact[ partnerContactPK=" + partnerContactPK + " ]";
    }

}
