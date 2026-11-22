package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_billing_invoice_mirror")
@Getter @Setter
public class PlatformBillingInvoiceMirrorEntity {
    @Id @Column(name = "source_invoice_id")
    private Long sourceInvoiceId;
    @Column(name = "source_tenant_id", nullable = false)
    private String sourceTenantId;
    @Column(name = "source_invoice_number", nullable = false)
    private String sourceInvoiceNumber;
    @Column(name = "source_status", nullable = false)
    private String sourceStatus;
    @Column(name = "currency", nullable = false)
    private String currency;
    @Column(name = "subtotal_cents", nullable = false)
    private long subtotalCents;
    @Column(name = "tax_cents", nullable = false)
    private long taxCents;
    @Column(name = "total_cents", nullable = false)
    private long totalCents;
    @Lob @Column(name = "snapshot_json", columnDefinition = "LONGTEXT", nullable = false)
    private String snapshotJson;
    @Column(name = "accounting_state", nullable = false)
    private String accountingState;
    @Column(name = "erp_invoice_id")
    private String erpInvoiceId;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
