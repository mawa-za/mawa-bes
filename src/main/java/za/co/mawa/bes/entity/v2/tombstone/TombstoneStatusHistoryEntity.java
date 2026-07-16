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
@Table(name="tombstone_status_history")
public class TombstoneStatusHistoryEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="tombstone_order_id", nullable=false) private String tombstoneOrderId;
    @Column(name="status_dimension", nullable=false, length=40) private String statusDimension;
    @Column(name="from_status", length=40) private String fromStatus;
    @Column(name="to_status", nullable=false, length=40) private String toStatus;
    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @Column(name="changed_at", nullable=false) private LocalDateTime changedAt;
    @Column(name="changed_by") private String changedBy;

    @PrePersist public void prePersist() { if (changedAt == null) changedAt = LocalDateTime.now(); }
}
