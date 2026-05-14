package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowEntity;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.Optional;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflowEntity, String> {

    Optional<ApprovalWorkflowEntity> findByApprovalTypeAndActiveTrue(ApprovalType approvalType);
}
