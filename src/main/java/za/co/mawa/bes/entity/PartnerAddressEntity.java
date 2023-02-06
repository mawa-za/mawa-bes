package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_address")
public class PartnerAddressEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerAddressPKEntity partnerAddressPK;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    public PartnerAddressEntity() {
    }

    public PartnerAddressEntity(PartnerAddressPKEntity partnerAddressPK) {
        this.partnerAddressPK = partnerAddressPK;
    }

    public PartnerAddressEntity(String partner, int addressId, String addressUsage) {
        this.partnerAddressPK = new PartnerAddressPKEntity(partner, addressId, addressUsage);
    }

    public PartnerAddressPKEntity getPartnerAddressPK() {
        return partnerAddressPK;
    }

    public void setPartnerAddressPK(PartnerAddressPKEntity partnerAddressPK) {
        this.partnerAddressPK = partnerAddressPK;
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
        hash += (partnerAddressPK != null ? partnerAddressPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerAddressEntity)) {
            return false;
        }
        PartnerAddressEntity other = (PartnerAddressEntity) object;
        if ((this.partnerAddressPK == null && other.partnerAddressPK != null) || (this.partnerAddressPK != null && !this.partnerAddressPK.equals(other.partnerAddressPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerAddress[ partnerAddressPK=" + partnerAddressPK + " ]";
    }
}
