package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;

import java.io.Serializable;

public class PartnerAddressPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner")
    private String partner;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "address_id")
    private int addressId;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "address_usage", length = 20)
    private String addressUsage;

    public PartnerAddressPKEntity() {
    }

    public PartnerAddressPKEntity(String partner, int addressId, String addressUsage) {
        this.partner = partner;
        this.addressId = addressId;
        this.addressUsage = addressUsage;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public int getAddressId() {
        return addressId;
    }

    public void setAddressId(int addressId) {
        this.addressId = addressId;
    }

    public String getAddressUsage() {
        return addressUsage;
    }

    public void setAddressUsage(String addressUsage) {
        this.addressUsage = addressUsage;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (partner != null ? partner.hashCode() : 0);
        hash += (int) addressId;
        hash += (addressUsage != null ? addressUsage.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerAddressPKEntity)) {
            return false;
        }
        PartnerAddressPKEntity other = (PartnerAddressPKEntity) object;
        if ((this.partner == null && other.partner != null) || (this.partner != null && !this.partner.equals(other.partner))) {
            return false;
        }
        if (this.addressId != other.addressId) {
            return false;
        }
        if ((this.addressUsage == null && other.addressUsage != null) || (this.addressUsage != null && !this.addressUsage.equals(other.addressUsage))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerAddressPK[ partner=" + partner + ", addressId=" + addressId + ", addressUsage=" + addressUsage + " ]";
    }

}
