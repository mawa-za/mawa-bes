package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
@Entity
@Table(name = "invoice")
public class InvoiceEntity {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "invoice_no", nullable = false, unique = true)
    private String invoiceNo;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "subtotal_cents", nullable = false)
    private Long subtotalCents = 0L;

    @Column(name = "tax_cents", nullable = false)
    private Long taxCents = 0L;

    @Column(name = "discount_cents", nullable = false)
    private Long discountCents = 0L;

    @Column(name = "total_cents", nullable = false)
    private Long totalCents = 0L;

    @Column(name = "paid_cents", nullable = false)
    private Long paidCents = 0L;

    @Column(name = "balance_cents", nullable = false)
    private Long balanceCents = 0L;

    @Column(name = "currency", nullable = false)
    private String currency = "ZAR";

    @Column(name = "notes")
    private String notes;

//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    // One-to-Many relationship with InvoiceLine
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineEntity> lines = new ArrayList<>();

    // One-to-Many relationship with InvoicePayment
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoicePaymentEntity> payments = new ArrayList<>();

}