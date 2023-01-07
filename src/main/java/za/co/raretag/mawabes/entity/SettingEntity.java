package za.co.raretag.mawabes.entity;

import jakarta.persistence.*;

import java.io.Serializable;


/**
 *
 * @author tebogomohale
 */
@Entity
@Table(name = "settings")
public class SettingEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SettingPKEntity settingsPKEntity;
    @Column(name = "value", length = 60)
    private String value;

    public SettingEntity() {
    }

    public SettingEntity(SettingPKEntity settingsPKEntity) {
        this.settingsPKEntity = settingsPKEntity;
    }

    public SettingEntity(String setting, String attribute) {
        this.settingsPKEntity = new SettingPKEntity(setting, attribute);
    }

    public SettingPKEntity getSettingsPK() {
        return settingsPKEntity;
    }

    public void setSettingsPK(SettingPKEntity settingsPKEntity) {
        this.settingsPKEntity = settingsPKEntity;
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
        hash += (settingsPKEntity != null ? settingsPKEntity.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SettingEntity)) {
            return false;
        }
        SettingEntity other = (SettingEntity) object;
        if ((this.settingsPKEntity == null && other.settingsPKEntity != null) || (this.settingsPKEntity != null && !this.settingsPKEntity.equals(other.settingsPKEntity))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.raretag.mawa.entities.SettingEntity[ SettingPKEntity=" + settingsPKEntity + " ]";
    }

}
