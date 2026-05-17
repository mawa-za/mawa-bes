package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowEntity;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.List;
import java.util.Optional;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflowEntity, String> {

    Optional<ApprovalWorkflowEntity> findByApprovalType(ApprovalType approvalType);

    Optional<ApprovalWorkflowEntity> findByApprovalTypeAndActiveTrue(ApprovalType approvalType);

    List<ApprovalWorkflowEntity> findByActiveTrueOrderByCreatedAtDesc();

    List<ApprovalWorkflowEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByApprovalType(ApprovalType approvalType);
}