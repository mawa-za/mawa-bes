package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
@Entity
@Table(name = "invoice_line")
public class InvoiceLineEntity {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Double quantity = 1.0;

    /** False for descriptive package-inclusion rows that must print with blank amount columns. */
    @Column(name = "show_amount", nullable = false)
    private Boolean showAmount = true;

    @Column(name = "unit_price_cents", nullable = false)
    private Long unitPriceCents = 0L;

    @Column(name = "discount_cents", nullable = false)
    private Long discountCents = 0L;

    /** Snapshotted line tax treatment; NULL indicates historical data not yet classified. */
    @Column(name = "vat_treatment_snapshot", length = 24)
    private String vatTreatmentSnapshot;

    /** Tax percentage as at invoice finalisation, not the tenant's current setting. */
    @Column(name = "vat_rate_percent_snapshot", precision = 9, scale = 4)
    private java.math.BigDecimal vatRatePercentSnapshot;

    /** Xero organisation tax type, resolved explicitly for this invoice line. */
    @Column(name = "xero_tax_type_snapshot", length = 100)
    private String xeroTaxTypeSnapshot;

    @Column(name = "tax_cents", nullable = false)
    private Long taxCents = 0L;

    @Column(name = "subtotal_cents", nullable = false)
    private Long subtotalCents = 0L;

    @Column(name = "total_cents", nullable = false)
    private Long totalCents = 0L;

//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;

    // Getters and Setters
    // (Omitted for brevity, implement based on need)
}