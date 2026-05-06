package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "membership")
public class MembershipEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @NotBlank
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    @NotBlank
    @Column(name = "membership_no", nullable = false, unique = true, length = 50)
    private String membershipNo;

    @NotBlank
    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @NotBlank
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "paid_up_to_period", length = 6)
    private String paidUpToPeriod;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "old_id")
    private String oldId;

    // Getters and Setters
}