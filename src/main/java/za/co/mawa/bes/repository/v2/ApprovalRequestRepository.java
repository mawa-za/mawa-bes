package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, String> {

    Optional<ApprovalRequestEntity> findByApprovalTypeAndReferenceId(
            ApprovalType approvalType,
            String referenceId
    );

    List<ApprovalRequestEntity> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);

    List<ApprovalRequestEntity> findByApprovalTypeOrderByCreatedAtDesc(ApprovalType approvalType);

    List<ApprovalRequestEntity> findByRequesterIdOrderByCreatedAtDesc(String requesterId);
}
