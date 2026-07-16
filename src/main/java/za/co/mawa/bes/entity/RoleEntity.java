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
@Builder
public class RoleEntity {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 45)
    private String id;
    @Column(name = "description", length = 255)
    private String description;
    @Column(name = "is_system_role", nullable = false)
    private Boolean systemRole = false;
    @Column(name = "is_protected", nullable = false)
    private Boolean protectedRole = false;
    @Column(name = "access_all_workcentres", nullable = false)
    private Boolean accessAllWorkcentres = false;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;


}
