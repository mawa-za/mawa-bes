package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.ApprovalActionType;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "approval_action")
public class ApprovalActionEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "approval_request_id", nullable = false)
    private String approvalRequestId;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalActionType action;

    @Column(name = "action_by", nullable = false)
    private String actionBy;

    @Column(name = "action_at", nullable = false)
    private Date actionAt = new Date();

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
