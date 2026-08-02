package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_type")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveTypeEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(nullable=false, unique=true, length=50) private String code;
    @Column(nullable=false, length=150) private String name;
    @Column(length=500) private String description;
    @Column(nullable=false) private Boolean paid;
    @Column(nullable=false, length=20) private String unit;
    @Column(name="allow_half_day", nullable=false) private Boolean allowHalfDay;
    @Column(name="requires_supporting_document", nullable=false) private Boolean requiresSupportingDocument;
    @Column(name="document_required_after", precision=10, scale=2) private BigDecimal documentRequiredAfter;
    @Column(name="minimum_request", nullable=false, precision=10, scale=2) private BigDecimal minimumRequest;
    @Column(name="maximum_consecutive", precision=10, scale=2) private BigDecimal maximumConsecutive;
    @Column(name="allow_negative_balance", nullable=false) private Boolean allowNegativeBalance;
    @Column(name="include_weekends", nullable=false) private Boolean includeWeekends;
    @Column(name="include_public_holidays", nullable=false) private Boolean includePublicHolidays;
    @Column(name="requires_approval", nullable=false) private Boolean requiresApproval;
    @Column(name="active_from", nullable=false) private LocalDate activeFrom;
    @Column(name="active_to", nullable=false) private LocalDate activeTo;
    @Column(name="display_order", nullable=false) private Integer displayOrder;
    @Column(length=20) private String colour;
    @Column(length=50) private String icon;
    @Column(nullable=false) private Boolean active;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at", insertable=false, updatable=false) private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;
    @Version @Column(nullable=false) private Long version;
}
