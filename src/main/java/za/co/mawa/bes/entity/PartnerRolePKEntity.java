package za.co.mawa.bes.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class PartnerRolePKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    @Column(name = "id", length = 20)
    private String id;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "role", length = 20)
    private String role;

    public PartnerRolePKEntity() {
    }

    public PartnerRolePKEntity(String id, String role) {
        this.id = id;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        hash += (role != null ? role.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PartnerRolePKEntity)) {
            return false;
        }
        PartnerRolePKEntity other = (PartnerRolePKEntity) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        if ((this.role == null && other.role != null) || (this.role != null && !this.role.equals(other.role))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.PartnerRolePK[ id=" + id + ", role=" + role + " ]";
    }

}
