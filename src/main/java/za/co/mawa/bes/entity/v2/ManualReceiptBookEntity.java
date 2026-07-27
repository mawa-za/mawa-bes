package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "manual_receipt_book", uniqueConstraints = {
        @UniqueConstraint(name = "uq_manual_receipt_book_no", columnNames = "receipt_book_no")
})
public class ManualReceiptBookEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 36)
    private String id;

    @Column(name = "receipt_book_no", nullable = false, length = 100)
    private String receiptBookNo;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "receipt_from_no", length = 100)
    private String receiptFromNo;

    @Column(name = "receipt_to_no", length = 100)
    private String receiptToNo;

    @Column(name = "assigned_employee_id", length = 255)
    private String assignedEmployeeId;

    @Column(name = "assigned_area_code", length = 100)
    private String assignedAreaCode;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) status = "ACTIVE";
        if (active == null) active = true;
        if (effectiveFrom == null) effectiveFrom = LocalDate.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
