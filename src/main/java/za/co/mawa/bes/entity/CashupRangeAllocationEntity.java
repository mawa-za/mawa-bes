package za.co.mawa.bes.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "cashup_range_allocation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_active",
                columnNames = {"device_id", "status"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashupRangeAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "from_no", nullable = false)
    private long fromNo;

    @Column(name = "to_no", nullable = false)
    private long toNo;

    @Column(name = "next_no", nullable = false)
    private long nextNo;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt = Instant.now();

}