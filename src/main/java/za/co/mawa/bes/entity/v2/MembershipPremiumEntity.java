package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.PremiumStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(
        name = "membership_premium",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_membership_premium_period",
                        columnNames = {"membership_id", "period_yyyymm"}
                )
        }
)
public class MembershipPremiumEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 255)
    private String id;

    @Column(name = "membership_id", nullable = false, length = 255)
    private String membershipId;

    @Column(name = "period_yyyymm", nullable = false, length = 6)
    private String periodYYYYMM;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "paid_amount_cents", nullable = false)
    private Long paidAmountCents = 0L;

    @Column(name = "balance_cents", nullable = false)
    private Long balanceCents = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PremiumStatus status = PremiumStatus.UNPAID;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}