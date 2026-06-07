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
@Table(name = "funeral_mortuary_inventory")
public class FuneralMortuaryInventoryEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "pickup_request_id")
    private String pickupRequestId;

    @Column(name = "deceased_partner_id")
    private String deceasedPartnerId;

    @Column(name = "deceased_name", nullable = false)
    private String deceasedName;

    @Column(name = "tag_number", nullable = false, unique = true)
    private String tagNumber;

    @Column(name = "check_in_date", nullable = false)
    private LocalDateTime checkInDate;

    @Column(name = "status", nullable = false)
    private String status = "IN_MORTUARY";

    @Column(name = "release_to")
    private String releaseTo;

    @Column(name = "identity_number")
    private String identityNumber;

    @Column(name = "checkout_date")
    private LocalDateTime checkoutDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (checkInDate == null) checkInDate = now;
        if (status == null) status = "IN_MORTUARY";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
