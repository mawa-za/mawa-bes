package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "appointment_service_order_line")
public class AppointmentServiceOrderLineEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false)
    private AppointmentServiceOrderEntity serviceOrder;

    @Column(name = "product_id", length = 36)
    private String productId;

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

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @PrePersist
    void prePersist() {
        if (quantity == null || quantity <= 0) quantity = 1.0;
        if (unitPriceCents == null) unitPriceCents = 0L;
        if (discountCents == null) discountCents = 0L;
        if (taxCents == null) taxCents = 0L;
        if (subtotalCents == null) subtotalCents = 0L;
        if (totalCents == null) totalCents = 0L;
        if (sortOrder == null) sortOrder = 0;
    }
}
