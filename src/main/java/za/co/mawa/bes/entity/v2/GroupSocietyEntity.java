package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "group_society")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSocietyEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private String partnerId;

    @Column(name = "group_no", nullable = false, unique = true)
    private String groupNo;

    @Column(name = "society_type")
    private String societyType;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "available_balance_cents", nullable = false)
    private Long availableBalanceCents = 0L;

    @Column(name = "total_paid_cents", nullable = false)
    private Long totalPaidCents = 0L;

    @Column(name = "total_claimed_cents", nullable = false)
    private Long totalClaimedCents = 0L;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "last_claim_date")
    private LocalDate lastClaimDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at", insertable = false)
    private Date updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

}