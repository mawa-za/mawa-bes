package za.co.mawa.bes.entity;


import jakarta.persistence.*;

import java.io.Serializable;


/**
 *
 * @author tebogomohale
 */
@Embeddable
public class UserRolePKEntity implements Serializable {

    @Column(name = "user", length = 200)
    private String user;
    @Column(name = "role", length = 20)
    private String role;

    public UserRolePKEntity() {
    }

    public UserRolePKEntity(String user, String role) {
        this.user = user;
        this.role = role;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
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
        hash += (user != null ? user.hashCode() : 0);
        hash += (role != null ? role.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserRolePKEntity)) {
            return false;
        }
        UserRolePKEntity other = (UserRolePKEntity) object;
        if ((this.user == null && other.user != null) || (this.user != null && !this.user.equals(other.user))) {
            return false;
        }
        if ((this.role == null && other.role != null) || (this.role != null && !this.role.equals(other.role))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.UserRolePKEntity[ user=" + user + ", role=" + role + " ]";
    }

}

