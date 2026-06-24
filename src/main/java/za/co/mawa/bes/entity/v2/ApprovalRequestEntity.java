package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "approval_request")
public class ApprovalRequestEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false)
    private ApprovalType approvalType;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "requester_id", nullable = false)
    private String requesterId;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "current_step_no", nullable = false)
    private Integer currentStepNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "payload_json", columnDefinition = "JSON")
    private String payloadJson;

    @Column(name = "final_action_by")
    private String finalActionBy;

    @Column(name = "final_action_at")
    private Date finalActionAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at", insertable = false)
    private Date updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
