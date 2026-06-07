package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;


import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "funeral_service_invoice")
public class FuneralServiceInvoiceEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "funeral_service_id", nullable = false)
    private String funeralServiceId;

    @Column(name = "invoice_id", nullable = false)
    private String invoiceId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "membership_claim_id")
    private String membershipClaimId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
