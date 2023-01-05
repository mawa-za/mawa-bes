package za.co.raretag.mawabes.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author tebogomohale
 */
@Embeddable
public class SettingPKEntity implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "setting", length = 20)
    private String setting;
    @Basic(optional = false)
    @NotNull
    @Column(name = "attribute", length = 60)
    private String attribute;

    public SettingPKEntity() {
    }

    public SettingPKEntity(String setting, String attribute) {
        this.setting = setting;
        this.attribute = attribute;
    }

    public String getSetting() {
        return setting;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (setting != null ? setting.hashCode() : 0);
        hash += (attribute != null ? attribute.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SettingPKEntity)) {
            return false;
        }
        SettingPKEntity other = (SettingPKEntity) object;
        if ((this.setting == null && other.setting != null) || (this.setting != null && !this.setting.equals(other.setting))) {
            return false;
        }
        if ((this.attribute == null && other.attribute != null) || (this.attribute != null && !this.attribute.equals(other.attribute))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.SettingPKEntity[ setting=" + setting + ", attribute=" + attribute + " ]";
    }

}

