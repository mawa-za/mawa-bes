package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;

import java.io.Serializable;

public class PartnerDatePKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner_no", length = 20)
    private String partner_no;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "type", length = 20)
    private String type;

    public PartnerDatePKEntity() {
    }

    public PartnerDatePKEntity(String partner_no, String type) {
        this.partner_no = partner_no;
        this.type = type;
    }

    public String getPartnerNumber() {
        return partner_no;
    }

    public void setPartnerNumber(String partner_no) {
        this.partner_no = partner_no;
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
        hash += (partner_no != null ? partner_no.hashCode() : 0);
        hash += (type != null ? type.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerDatePKEntity)) {
            return false;
        }
        PartnerDatePKEntity other = (PartnerDatePKEntity) object;
        if ((this.partner_no == null && other.partner_no != null) || (this.partner_no != null && !this.partner_no.equals(other.partner_no))) {
            return false;
        }
        if ((this.type == null && other.type != null) || (this.type != null && !this.type.equals(other.type))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.co.raretag.mawa.entities.PartnerDatePK[ partner_no =" + partner_no + ", type=" + type + " ]";
    }
}
