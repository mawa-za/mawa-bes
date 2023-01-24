package za.co.mawa.bes.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class RoleWorkcenterPKEntity implements Serializable {
    private String role;

    private String workcenter;
}
