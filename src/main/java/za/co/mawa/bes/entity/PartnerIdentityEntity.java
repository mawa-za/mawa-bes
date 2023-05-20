package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_identity")
public class PartnerIdentityEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerIdentityPKEntity partnerIdentityPK;
    @Column(name = "partner")
    private String partner;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    public PartnerIdentityEntity() {
    }

    public PartnerIdentityEntity(PartnerIdentityPKEntity partnerIdentityPK) {
        this.partnerIdentityPK = partnerIdentityPK;
    }

    public PartnerIdentityEntity(String partner, String type) {
        this.partnerIdentityPK = new PartnerIdentityPKEntity(partner, type);
    }

    public PartnerIdentityPKEntity getPartnerIdentityPK() {
        return partnerIdentityPK;
    }

    public void setPartnerIdentityPK(PartnerIdentityPKEntity partnerIdentityPK) {
        this.partnerIdentityPK = partnerIdentityPK;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
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
        hash += (partnerIdentityPK != null ? partnerIdentityPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerIdentityEntity)) {
            return false;
        }
        PartnerIdentityEntity other = (PartnerIdentityEntity) object;
        if ((this.partnerIdentityPK == null && other.partnerIdentityPK != null) || (this.partnerIdentityPK != null && !this.partnerIdentityPK.equals(other.partnerIdentityPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerIdentity[ partnerIdentityPK=" + partnerIdentityPK + " ]";
    }

}
