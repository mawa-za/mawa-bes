package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class PartnerRelationPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner1", length = 20)
    private String partner1;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner2", length = 20)
    private String partner2;

    public PartnerRelationPKEntity() {
    }

    public PartnerRelationPKEntity(String type, String partner1, String partner2) {
        this.type = type;
        this.partner1 = partner1;
        this.partner2 = partner2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPartner1() {
        return partner1;
    }

    public void setPartner1(String partner1) {
        this.partner1 = partner1;
    }

    public String getPartner2() {
        return partner2;
    }

    public void setPartner2(String partner2) {
        this.partner2 = partner2;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (type != null ? type.hashCode() : 0);
        hash += (partner1 != null ? partner1.hashCode() : 0);
        hash += (partner2 != null ? partner2.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerRelationPKEntity)) {
            return false;
        }
        PartnerRelationPKEntity other = (PartnerRelationPKEntity) object;
        if ((this.type == null && other.type != null) || (this.type != null && !this.type.equals(other.type))) {
            return false;
        }
        if ((this.partner1 == null && other.partner1 != null) || (this.partner1 != null && !this.partner1.equals(other.partner1))) {
            return false;
        }
        if ((this.partner2 == null && other.partner2 != null) || (this.partner2 != null && !this.partner2.equals(other.partner2))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerRelationPK[ type=" + type + ", partner1=" + partner1 + ", partner2=" + partner2 + " ]";
    }

}
