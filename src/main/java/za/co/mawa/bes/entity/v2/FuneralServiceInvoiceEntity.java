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
@Builder
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

    @Column(name = "group_society_claim_id")
    private String groupSocietyClaimId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents = 0L;


    @Column(name = "membership_holder_name") private String membershipHolderName;
    @Column(name = "membership_holder_identity") private String membershipHolderIdentity;
    @Column(name = "deceased_name") private String deceasedName;
    @Column(name = "deceased_identity") private String deceasedIdentity;
    @Column(name = "provider_tenant_id") private String providerTenantId;
    @Column(name = "cover_tenant_id") private String coverTenantId;
    @Column(name = "payment_request_id") private String paymentRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
