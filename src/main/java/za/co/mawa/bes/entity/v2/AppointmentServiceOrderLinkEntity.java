package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "appointment_service_order_link")
public class AppointmentServiceOrderLinkEntity {
    @Id
    @Column(name = "appointment_id", length = 36)
    private String appointmentId;

    @Column(name = "service_order_id", nullable = false, unique = true, length = 36)
    private String serviceOrderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
