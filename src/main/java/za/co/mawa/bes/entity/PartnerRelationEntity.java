package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_relation")
public class PartnerRelationEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerRelationPKEntity partnerRelationPK;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    public PartnerRelationEntity() {
    }

    public PartnerRelationEntity(PartnerRelationPKEntity partnerRelationPK) {
        this.partnerRelationPK = partnerRelationPK;
    }

    public PartnerRelationEntity(String type, String partner1, String partner2) {
        this.partnerRelationPK = new PartnerRelationPKEntity(type, partner1, partner2);
    }

    public PartnerRelationPKEntity getPartnerRelationPK() {
        return partnerRelationPK;
    }

    public void setPartnerRelationPK(PartnerRelationPKEntity partnerRelationPK) {
        this.partnerRelationPK = partnerRelationPK;
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
        hash += (partnerRelationPK != null ? partnerRelationPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerRelationEntity)) {
            return false;
        }
        PartnerRelationEntity other = (PartnerRelationEntity) object;
        if ((this.partnerRelationPK == null && other.partnerRelationPK != null) || (this.partnerRelationPK != null && !this.partnerRelationPK.equals(other.partnerRelationPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerRelation[ partnerRelationPK=" + partnerRelationPK + " ]";
    }

}
