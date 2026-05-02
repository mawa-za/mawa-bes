package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "invoice_payment")
public class InvoicePaymentEntity {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    // Getters and Setters
    // (Omitted for brevity, implement based on need)
}