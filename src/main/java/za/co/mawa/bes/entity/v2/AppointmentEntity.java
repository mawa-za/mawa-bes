package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "appointment")
public class AppointmentEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "appointment_no", nullable = false, unique = true, length = 50)
    private String appointmentNo;

    @Column(name = "customer_partner_id", nullable = false, length = 36)
    private String customerPartnerId;

    @Column(name = "employee_partner_id", length = 36)
    private String employeePartnerId;

    @Column(name = "responsible_user_id", length = 36)
    private String responsibleUserId;

    @Column(name = "service_product_id", length = 36)
    private String serviceProductId;

    @Column(name = "service_location_id", length = 36)
    private String serviceLocationId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "BOOKED";

    @Column(name = "location")
    private String location;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id", length = 36)
    private String sourceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null || status.isBlank()) status = "BOOKED";
        if (appointmentNo == null || appointmentNo.isBlank()) appointmentNo = "APT-" + System.currentTimeMillis();
        if (durationMinutes == null && startTime != null && endTime != null) {
            durationMinutes = Math.toIntExact(java.time.Duration.between(startTime, endTime).toMinutes());
        }
        if (endTime == null && startTime != null && durationMinutes != null && durationMinutes > 0) {
            endTime = startTime.plusMinutes(durationMinutes);
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (endTime == null && startTime != null && durationMinutes != null && durationMinutes > 0) {
            endTime = startTime.plusMinutes(durationMinutes);
        }
    }
}
