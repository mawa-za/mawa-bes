package za.co.mawa.bes.entity.v2.tombstone;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="tombstone_funding_allocation")
public class TombstoneFundingAllocationEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="tombstone_order_id", nullable=false) private String tombstoneOrderId;
    @Column(name="funding_type", nullable=false, length=30) private String fundingType;
    @Column(name="source_type", nullable=false, length=50) private String sourceType;
    @Column(name="source_id") private String sourceId;
    @Column(name="source_no", length=150) private String sourceNo;
    @Column(name="allocated_amount_cents", nullable=false) private Long allocatedAmountCents = 0L;
    @Column(name="confirmed_amount_cents", nullable=false) private Long confirmedAmountCents = 0L;
    @Column(name="status", nullable=false, length=30) private String status = "PENDING";
    @Column(name="confirmed_at") private LocalDateTime confirmedAt;
    @Column(name="notes", columnDefinition="TEXT") private String notes;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
