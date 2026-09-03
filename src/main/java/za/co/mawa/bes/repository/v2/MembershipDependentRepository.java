package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.enums.MembershipDependentStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MembershipDependentRepository extends JpaRepository<MembershipDependentEntity, String> {
    List<MembershipDependentEntity> findByMembershipId(String membershipId);

    List<MembershipDependentEntity> findByMembershipIdAndStatusInOrderByEffectiveFromAsc(
            String membershipId,
            Collection<MembershipDependentStatus> statuses
    );

    List<MembershipDependentEntity> findByMembershipIdAndStatus(
            String membershipId,
            MembershipDependentStatus status
    );

    long countByMembershipIdAndStatusIn(
            String membershipId,
            Collection<MembershipDependentStatus> statuses
    );

    Optional<MembershipDependentEntity> findByIdAndMembershipId(String id, String membershipId);

    Optional<MembershipDependentEntity> findFirstByMembershipIdAndDependentPartnerIdOrderByCreatedAtDesc(
            String membershipId,
            String dependentPartnerId
    );

    boolean existsByMembershipIdAndDependentPartnerId(String membershipId, String dependentPartnerId);

    boolean existsByMembershipIdAndDependentPartnerIdAndStatus(
            String membershipId,
            String dependentPartnerId,
            MembershipDependentStatus status
    );

    @Query("""
            SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
              FROM MembershipDependentEntity d
             WHERE d.membershipId = :membershipId
               AND d.dependentPartnerId = :partnerId
               AND d.status IN :statuses
            """)
    boolean existsVisibleByMembershipIdAndPartnerId(
            @Param("membershipId") String membershipId,
            @Param("partnerId") String partnerId,
            @Param("statuses") Collection<MembershipDependentStatus> statuses
    );
}
