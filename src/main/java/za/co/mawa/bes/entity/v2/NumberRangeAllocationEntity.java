package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "number_range_allocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NumberRangeAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seq_type", nullable = false, length = 64)
    private String seqType;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "from_no", nullable = false)
    private Long fromNo;

    @Column(name = "to_no", nullable = false)
    private Long toNo;

    @Column(name = "next_local_no", nullable = false)
    private Long nextLocalNo;

    @Column(name = "allocation_size", nullable = false)
    private Integer allocationSize;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

}