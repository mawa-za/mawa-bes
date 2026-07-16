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
@Table(name="tombstone_order_item")
public class TombstoneOrderItemEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="tombstone_order_id", nullable=false) private String tombstoneOrderId;
    @Column(name="product_id") private String productId;
    @Column(name="item_type", nullable=false, length=50) private String itemType = "TOMBSTONE";
    @Column(name="description", nullable=false, length=500) private String description;
    @Column(name="material", length=100) private String material;
    @Column(name="colour", length=100) private String colour;
    @Column(name="dimensions") private String dimensions;
    @Column(name="inscription_text", columnDefinition="TEXT") private String inscriptionText;
    @Column(name="quantity", precision=12, scale=3, nullable=false) private BigDecimal quantity = BigDecimal.ONE;
    @Column(name="unit_price_cents", nullable=false) private Long unitPriceCents = 0L;
    @Column(name="discount_cents", nullable=false) private Long discountCents = 0L;
    @Column(name="tax_cents", nullable=false) private Long taxCents = 0L;
    @Column(name="total_cents", nullable=false) private Long totalCents = 0L;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
