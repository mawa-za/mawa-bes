package za.co.raretag.mawabes.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

public class TenantPropertyEntityId implements Serializable {

    private String tenant;
    private String property;

    public TenantPropertyEntityId() {
    }

    public TenantPropertyEntityId(String tenant, String property) {
        this.tenant = tenant;
        this.property = property;
    }
}
