package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "number_sequence",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_number_sequence_type", columnNames = "seq_type")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NumberSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seq_type", nullable = false, length = 64)
    private String seqType;

    @Column(name = "description", length = 160)
    private String description;

    @Column(name = "start_no", nullable = false)
    private Long startNo;

    @Column(name = "next_no", nullable = false)
    private Long nextNo;

    @Column(name = "end_no", nullable = false)
    private Long endNo;

    @Column(name = "default_allocation_size", nullable = false)
    private Integer defaultAllocationSize;

    @Column(name = "warning_threshold", nullable = false)
    private Long warningThreshold;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 64, updatable = false)
    private String createdBy;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;
}
