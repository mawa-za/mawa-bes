package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "membership_change_audit")
public class MembershipChangeAuditEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "membership_id", nullable = false, length = 255)
    private String membershipId;

    @Column(name = "change_request_id", length = 255)
    private String changeRequestId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "old_values_json", columnDefinition = "JSON")
    private String oldValuesJson;

    @Column(name = "new_values_json", columnDefinition = "JSON")
    private String newValuesJson;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
