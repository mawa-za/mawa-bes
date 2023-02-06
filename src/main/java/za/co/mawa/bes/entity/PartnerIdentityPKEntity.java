package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;

import java.io.Serializable;

public class PartnerIdentityPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "value", length = 60)
    private String value;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;

    public PartnerIdentityPKEntity() {
    }

    public PartnerIdentityPKEntity(String value, String type) {
        this.value = value;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (value != null ? value.hashCode() : 0);
        hash += (type != null ? type.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerIdentityPKEntity)) {
            return false;
        }
        PartnerIdentityPKEntity other = (PartnerIdentityPKEntity) object;
        if (this.value != other.value) {
            return false;
        }
        if ((this.type == null && other.type != null) || (this.type != null && !this.type.equals(other.type))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerIdentityPK[ value=" + value + ", type=" + type + " ]";
    }
}
