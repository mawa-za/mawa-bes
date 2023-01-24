package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_role")
public class PartnerRoleEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PartnerRolePKEntity partnerRolePK;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    public PartnerRoleEntity() {
    }

    public PartnerRoleEntity(PartnerRolePKEntity partnerRolePK) {
        this.partnerRolePK = partnerRolePK;
    }

    public PartnerRoleEntity(String id, String role) {
        this.partnerRolePK = new PartnerRolePKEntity(id, role);
    }

    public PartnerRolePKEntity getPartnerRolePK() {
        return partnerRolePK;
    }

    public void setPartnerRolePK(PartnerRolePKEntity partnerRolePK) {
        this.partnerRolePK = partnerRolePK;
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
        hash += (partnerRolePK != null ? partnerRolePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerRoleEntity)) {
            return false;
        }
        PartnerRoleEntity other = (PartnerRoleEntity) object;
        if ((this.partnerRolePK == null && other.partnerRolePK != null) || (this.partnerRolePK != null && !this.partnerRolePK.equals(other.partnerRolePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerRole[ partnerRolePK=" + partnerRolePK + " ]";
    }

}
