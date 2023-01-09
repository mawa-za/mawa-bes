package za.co.raretag.mawabes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author tebogomohale
 */
@Entity
@Table(name = "user_role")
public class UserRoleEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected UserRolePKEntity userRolePKEntity;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    public UserRoleEntity() {
    }

    public UserRoleEntity(UserRolePKEntity userRolePKEntity) {
        this.userRolePKEntity = userRolePKEntity;
    }

    public UserRoleEntity(String user, String role) {
        this.userRolePKEntity = new UserRolePKEntity(user, role);
    }

    public UserRolePKEntity getUserRolePKEntity() {
        return userRolePKEntity;
    }

    public void setUserRolePKEntity(UserRolePKEntity userRolePKEntity) {
        this.userRolePKEntity = userRolePKEntity;
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
        hash += (userRolePKEntity != null ? userRolePKEntity.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserRoleEntity)) {
            return false;
        }
        UserRoleEntity other = (UserRoleEntity) object;
        if ((this.userRolePKEntity == null && other.userRolePKEntity != null) || (this.userRolePKEntity != null && !this.userRolePKEntity.equals(other.userRolePKEntity))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.UserRoleEntity[ userRolePKEntity=" + userRolePKEntity + " ]";
    }

}

