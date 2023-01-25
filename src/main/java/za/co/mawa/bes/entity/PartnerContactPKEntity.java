package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;

import java.io.Serializable;

public class PartnerContactPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner", length = 20)
    private String partner;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;

    public PartnerContactPKEntity() {
    }

    public PartnerContactPKEntity(String partner, String type) {
        this.partner = partner;
        this.type = type;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (partner != null ? partner.hashCode() : 0);
        hash += (type != null ? type.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerContactPKEntity)) {
            return false;
        }
        PartnerContactPKEntity other = (PartnerContactPKEntity) object;
        if ((this.partner == null && other.partner != null) || (this.partner != null && !this.partner.equals(other.partner))) {
            return false;
        }
        if ((this.type == null && other.type != null) || (this.type != null && !this.type.equals(other.type))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerContactPK[ partner=" + partner + ", type=" + type + " ]";
    }

}
