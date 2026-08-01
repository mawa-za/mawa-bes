package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "service_order_line")
public class ServiceOrderLineEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false)
    private ServiceOrderEntity serviceOrder;

    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(name = "item_type", nullable = false, length = 30)
    private String itemType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private Long unitPriceCents;

    @Column(name = "discount_cents", nullable = false)
    private Long discountCents;

    @Column(name = "tax_cents", nullable = false)
    private Long taxCents;

    @Column(name = "subtotal_cents", nullable = false)
    private Long subtotalCents;

    @Column(name = "total_cents", nullable = false)
    private Long totalCents;

    @Column(name = "employee_partner_id", length = 36)
    private String employeePartnerId;

    @Column(name = "scheduled_start_at")
    private LocalDateTime scheduledStartAt;

    @Column(name = "scheduled_end_at")
    private LocalDateTime scheduledEndAt;

    @Column(name = "completion_status", nullable = false, length = 30)
    private String completionStatus;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @PrePersist
    void prePersist() {
        if (itemType == null || itemType.isBlank()) itemType = "SERVICE";
        if (quantity == null || quantity <= 0) quantity = 1.0;
        if (unitPriceCents == null) unitPriceCents = 0L;
        if (discountCents == null) discountCents = 0L;
        if (taxCents == null) taxCents = 0L;
        if (subtotalCents == null) subtotalCents = 0L;
        if (totalCents == null) totalCents = 0L;
        if (completionStatus == null || completionStatus.isBlank()) completionStatus = "NOT_STARTED";
        if (sortOrder == null) sortOrder = 0;
    }
}
