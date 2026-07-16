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
@Table(name="tombstone_installation_material")
public class TombstoneInstallationMaterialEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="installation_id", nullable=false) private String installationId;
    @Column(name="product_id") private String productId;
    @Column(name="description", nullable=false, length=500) private String description;
    @Column(name="quantity", precision=12, scale=3, nullable=false) private BigDecimal quantity = BigDecimal.ONE;
    @Column(name="uom", length=30) private String uom;
    @Column(name="consumed_quantity", precision=12, scale=3, nullable=false) private BigDecimal consumedQuantity = BigDecimal.ZERO;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
