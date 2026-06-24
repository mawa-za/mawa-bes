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
@Table(name = "funeral_pickup_request")
public class FuneralPickupRequestEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "deceased_name", nullable = false)
    private String deceasedName;

    @Column(name = "pickup_location", nullable = false, length = 1000)
    private String pickupLocation;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "assigned_staff_id")
    private String assignedStaffId;

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "mortuary_inventory_id")
    private String mortuaryInventoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
