package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepEntity;

import java.util.List;
import java.util.Optional;

public interface ApprovalWorkflowStepRepository extends JpaRepository<ApprovalWorkflowStepEntity, String> {

    List<ApprovalWorkflowStepEntity> findByWorkflowIdAndActiveTrueOrderByStepNoAsc(String workflowId);

    Optional<ApprovalWorkflowStepEntity> findByWorkflowIdAndStepNoAndActiveTrue(String workflowId, Integer stepNo);
}
