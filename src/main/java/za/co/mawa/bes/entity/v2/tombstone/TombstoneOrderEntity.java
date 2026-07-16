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
@Table(name="tombstone_order")
public class TombstoneOrderEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="order_no", nullable=false, unique=true, length=50) private String orderNo;
    @Column(name="customer_partner_id", nullable=false) private String customerPartnerId;
    @Column(name="membership_id") private String membershipId;
    @Column(name="deceased_partner_id") private String deceasedPartnerId;
    @Column(name="deceased_name", nullable=false) private String deceasedName;
    @Column(name="funeral_service_id") private String funeralServiceId;
    @Column(name="cemetery_name") private String cemeteryName;
    @Column(name="cemetery_area") private String cemeteryArea;
    @Column(name="grave_number", length=100) private String graveNumber;
    @Column(name="grave_latitude", precision=10, scale=7) private BigDecimal graveLatitude;
    @Column(name="grave_longitude", precision=10, scale=7) private BigDecimal graveLongitude;
    @Column(name="sales_area", length=100) private String salesArea;
    @Column(name="workcenter_id") private String workcenterId;
    @Column(name="expected_installation_date") private LocalDate expectedInstallationDate;
    @Column(name="funding_method", nullable=false, length=30) private String fundingMethod;
    @Column(name="status", nullable=false, length=40) private String status = "DRAFT";
    @Column(name="funding_status", nullable=false, length=30) private String fundingStatus = "UNFUNDED";
    @Column(name="production_status", nullable=false, length=40) private String productionStatus = "NOT_STARTED";
    @Column(name="installation_status", nullable=false, length=40) private String installationStatus = "NOT_READY";
    @Column(name="subtotal_cents", nullable=false) private Long subtotalCents = 0L;
    @Column(name="tax_cents", nullable=false) private Long taxCents = 0L;
    @Column(name="discount_cents", nullable=false) private Long discountCents = 0L;
    @Column(name="total_cents", nullable=false) private Long totalCents = 0L;
    @Column(name="confirmed_funding_cents", nullable=false) private Long confirmedFundingCents = 0L;
    @Column(name="balance_cents", nullable=false) private Long balanceCents = 0L;
    @Column(name="invoice_id") private String invoiceId;
    @Column(name="notes", columnDefinition="TEXT") private String notes;
    @Column(name="cancellation_reason", columnDefinition="TEXT") private String cancellationReason;
    @Column(name="cancelled_at") private LocalDateTime cancelledAt;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
