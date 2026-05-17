package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import za.co.mawa.bes.enums.ApproverType;

import java.util.List;

public interface ApprovalWorkflowStepApproverRepository extends JpaRepository<ApprovalWorkflowStepApproverEntity, String> {

    List<ApprovalWorkflowStepApproverEntity> findByWorkflowStepIdAndActiveTrue(String workflowStepId);

    List<ApprovalWorkflowStepApproverEntity> findByApproverTypeAndApproverValueAndActiveTrue(
            ApproverType approverType,
            String approverValue
    );
}