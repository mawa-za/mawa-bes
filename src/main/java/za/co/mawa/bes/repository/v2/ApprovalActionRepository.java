package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ApprovalActionEntity;
import za.co.mawa.bes.enums.ApprovalActionType;

import java.util.List;

public interface ApprovalActionRepository extends JpaRepository<ApprovalActionEntity, String> {

    List<ApprovalActionEntity> findByApprovalRequestIdOrderByActionAtAsc(String approvalRequestId);

    long countByApprovalRequestIdAndStepNoAndAction(
            String approvalRequestId,
            Integer stepNo,
            ApprovalActionType action
    );

    boolean existsByApprovalRequestIdAndStepNoAndActionByAndAction(
            String approvalRequestId,
            Integer stepNo,
            String actionBy,
            ApprovalActionType action
    );
}
