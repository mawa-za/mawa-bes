package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "service_order")
public class ServiceOrderEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "service_order_no", nullable = false, unique = true, length = 50)
    private String serviceOrderNo;

    @Column(name = "customer_partner_id", nullable = false, length = 36)
    private String customerPartnerId;

    @Column(name = "assigned_employee_partner_id", length = 36)
    private String assignedEmployeePartnerId;

    @Column(name = "sales_area_id", length = 36)
    private String salesAreaId;

    @Column(name = "service_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "scheduled_start_at")
    private LocalDateTime scheduledStartAt;

    @Column(name = "scheduled_end_at")
    private LocalDateTime scheduledEndAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "location")
    private String location;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "subtotal_cents", nullable = false)
    private Long subtotalCents;

    @Column(name = "discount_cents", nullable = false)
    private Long discountCents;

    @Column(name = "tax_cents", nullable = false)
    private Long taxCents;

    @Column(name = "total_cents", nullable = false)
    private Long totalCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Column(name = "invoice_status", nullable = false, length = 30)
    private String invoiceStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ServiceOrderLineEntity> lines = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (orderDate == null) orderDate = LocalDate.now();
        if (status == null || status.isBlank()) status = "DRAFT";
        if (subtotalCents == null) subtotalCents = 0L;
        if (discountCents == null) discountCents = 0L;
        if (taxCents == null) taxCents = 0L;
        if (totalCents == null) totalCents = 0L;
        if (currency == null || currency.isBlank()) currency = "ZAR";
        if (invoiceStatus == null || invoiceStatus.isBlank()) invoiceStatus = "NOT_INVOICED";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
