package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.Objects;
@Entity
@Table(name = "role")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class RoleEntity {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 45)
    private String id;
    @Column(name = "description", length = 60)
    private String description;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;


}
