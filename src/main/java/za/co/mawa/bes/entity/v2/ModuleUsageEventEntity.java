package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "module_usage_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleUsageEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "module_code", nullable = false, length = 100)
    private String moduleCode;

    @Column(name = "module_name", length = 150)
    private String moduleName;

    @Column(name = "module_path")
    private String modulePath;

    @Column(name = "workcenter_id")
    private String workcenterId;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        LocalDateTime now = LocalDateTime.now();

        if (usedAt == null) {
            usedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }

}
