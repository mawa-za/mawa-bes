package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "membership_dependent", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"membership_id", "dependent_partner_id"})
})
public class MembershipDependentEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @NotBlank
    @Column(name = "membership_id", nullable = false, length = 36)
    private String membershipId;

    @NotBlank
    @Column(name = "dependent_partner_id", nullable = false, length = 36)
    private String dependentPartnerId;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String relationship;

    @NotNull
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    // Getters and Setters
}