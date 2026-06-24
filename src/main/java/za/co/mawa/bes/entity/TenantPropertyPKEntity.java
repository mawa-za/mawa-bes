package za.co.mawa.bes.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class TenantPropertyPKEntity implements Serializable {
    private String tenant;
    private String property;

}
