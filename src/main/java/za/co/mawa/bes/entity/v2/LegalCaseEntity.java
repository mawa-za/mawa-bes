package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "legal_case")
public class LegalCaseEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "case_no", nullable = false, unique = true, length = 80)
    private String caseNo;
    @Column(nullable = false)
    private String title;
    @Column(name = "client_partner_id", nullable = false)
    private String clientPartnerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false)
    private LegalCaseType caseType;
    @Column(name = "case_category")
    private String caseCategory;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LegalCaseStatus status = LegalCaseStatus.OPEN;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CasePriority priority = CasePriority.NORMAL;
    @Column(name = "assigned_to")
    private String assignedTo;
    @Column(name = "opened_date", nullable = false)
    private LocalDate openedDate;
    @Column(name = "closed_date")
    private LocalDate closedDate;
    @Column(name = "court_name")
    private String courtName;
    @Column(name = "court_case_no")
    private String courtCaseNo;
    @Column(name = "forum_type")
    private String forumType;
    @Column(name = "next_appearance_date")
    private LocalDateTime nextAppearanceDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", nullable = false)
    private CaseBillingType billingType = CaseBillingType.HOURLY;
    @Column(name = "hourly_rate_cents")
    private Long hourlyRateCents;
    @Column(name = "fixed_fee_cents")
    private Long fixedFeeCents;
    @Column(nullable = false)
    private String currency = "ZAR";
    @Column(nullable = false)
    private Boolean billable = true;
    @Column(name = "total_time_minutes", nullable = false)
    private Long totalTimeMinutes = 0L;
    @Column(name = "total_fees_cents", nullable = false)
    private Long totalFeesCents = 0L;
    @Column(name = "total_disbursements_cents", nullable = false)
    private Long totalDisbursementsCents = 0L;
    @Column(name = "total_billable_cents", nullable = false)
    private Long totalBillableCents = 0L;
    @Column(name = "total_billed_cents", nullable = false)
    private Long totalBilledCents = 0L;
    @Column(name = "balance_cents", nullable = false)
    private Long balanceCents = 0L;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "updated_by")
    private String updatedBy;
}
