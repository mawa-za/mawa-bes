package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "membership_change_configuration")
public class MembershipChangeConfigurationEntity {
    @Id
    @Column(name = "id", length = 30)
    private String id;

    @Column(name = "plan_change_waiting_period_months", nullable = false)
    private Integer planChangeWaitingPeriodMonths;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}
