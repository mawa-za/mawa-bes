package za.co.mawa.bes.repository.v2;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<ApprovalRequestEntity> findTopByApprovalTypeAndReferenceIdOrderByCreatedAtDesc(
            ApprovalType approvalType,
            String referenceId
    );

    List<ApprovalRequestEntity> findByReferenceId(String referenceId);

    @Query(value = """
        SELECT ar.* FROM approval_request ar
         WHERE ar.reference_id = :membershipId
            OR ar.reference_id IN (SELECT mc.id FROM membership_change_request mc
                                   WHERE mc.membership_id = :membershipId)
            OR JSON_UNQUOTE(JSON_EXTRACT(ar.payload_json, '$.membershipId')) = :membershipId
         ORDER BY ar.created_at DESC
        """, nativeQuery = true)
    List<ApprovalRequestEntity> findHistoryForMembership(@Param("membershipId") String membershipId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ApprovalRequestEntity request where request.id = :id")
    Optional<ApprovalRequestEntity> findByIdForUpdate(@Param("id") String id);

    List<ApprovalRequestEntity> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);

    List<ApprovalRequestEntity> findByApprovalTypeOrderByCreatedAtDesc(ApprovalType approvalType);

    List<ApprovalRequestEntity> findByRequesterIdOrderByCreatedAtDesc(String requesterId);

    List<ApprovalRequestEntity> findByStatusAndApprovalTypeOrderByCreatedAtDesc(
            ApprovalStatus status,
            ApprovalType approvalType
    );

    List<ApprovalRequestEntity> findAllByOrderByCreatedAtDesc();
}
