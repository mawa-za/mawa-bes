package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_profile_rule")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveProfileRuleEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name="leave_profile_id", nullable=false) private String leaveProfileId;
    @Column(name="leave_type_id", nullable=false) private String leaveTypeId;
    @Column(name="entitlement_amount", nullable=false, precision=10, scale=2) private BigDecimal entitlementAmount;
    @Column(name="cycle_months", nullable=false) private Integer cycleMonths;
    @Column(name="accrual_method", nullable=false, length=30) private String accrualMethod;
    @Column(name="accrual_frequency", nullable=false, length=30) private String accrualFrequency;
    @Column(name="accrual_amount", precision=10, scale=4) private BigDecimal accrualAmount;
    @Column(name="pro_rata", nullable=false) private Boolean proRata;
    @Column(name="carry_over_allowed", nullable=false) private Boolean carryOverAllowed;
    @Column(name="maximum_carry_over", precision=10, scale=2) private BigDecimal maximumCarryOver;
    @Column(name="carry_over_expiry_months") private Integer carryOverExpiryMonths;
    @Column(name="maximum_negative_balance", nullable=false, precision=10, scale=2) private BigDecimal maximumNegativeBalance;
    @Column(name="waiting_period_days", nullable=false) private Integer waitingPeriodDays;
    @Column(name="supporting_document_required_override") private Boolean supportingDocumentRequiredOverride;
    @Column(name="active_from", nullable=false) private LocalDate activeFrom;
    @Column(name="active_to", nullable=false) private LocalDate activeTo;
    @Column(nullable=false) private Boolean active;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at", insertable=false, updatable=false) private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;
    @Version @Column(nullable=false) private Long version;
}
