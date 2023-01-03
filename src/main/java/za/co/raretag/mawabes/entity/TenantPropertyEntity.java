package za.co.raretag.mawabes.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tenant_property")
@IdClass(TenantPropertyEntityId.class)
public class TenantPropertyEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "tenant")
    private String tenant;

    @Id
    @Column(name = "property")
    private String property;

    @Column(name = "value")
    private String value;

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
