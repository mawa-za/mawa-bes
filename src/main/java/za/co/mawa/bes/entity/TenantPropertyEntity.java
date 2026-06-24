package za.co.mawa.bes.entity;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "tenant_property")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class TenantPropertyEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TenantPropertyPKEntity tenantPropertyPKEntity;
    @Column(name = "value")
    private String value;

}
