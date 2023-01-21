package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "role_workcenter")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class RoleWorkcenterEntity implements Serializable {
    @EmbeddedId
    RoleWorkcenterPKEntity roleWorkcenterPKEntity;

    @Column(name = "position")
    private int position;
}
