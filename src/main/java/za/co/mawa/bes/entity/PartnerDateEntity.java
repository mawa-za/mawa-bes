package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_date")
public class PartnerDateEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerDatePKEntity partnerDatePK;
    @Column(name = "value")
    @Temporal(TemporalType.TIMESTAMP)
    private Date value;

    public PartnerDateEntity() {
    }

    public PartnerDateEntity(PartnerDatePKEntity partnerDatePK) {
        this.partnerDatePK = partnerDatePK;
    }

    public PartnerDateEntity(String partner_no, String type) {
        this.partnerDatePK = new PartnerDatePKEntity(partner_no, type);
    }

    public PartnerDatePKEntity getPartnerDatePK() {
        return partnerDatePK;
    }

    public void setPartnerDatePK(PartnerDatePKEntity partnerDatePK) {
        this.partnerDatePK = partnerDatePK;
    }

    public Date getValue() {
        return value;
    }

    public void setValue(Date value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (partnerDatePK != null ? partnerDatePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerDateEntity)) {
            return false;
        }
        PartnerDateEntity other = (PartnerDateEntity) object;
        if ((this.partnerDatePK == null && other.partnerDatePK != null) || (this.partnerDatePK != null && !this.partnerDatePK.equals(other.partnerDatePK))) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "za.co.raretag.mawa.entities.PartnerDate[ partnerDatePK=" + partnerDatePK + " ]";
    }
}
