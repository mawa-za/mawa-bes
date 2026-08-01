package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipPlanClaimPayoutEntity;
import za.co.mawa.bes.enums.ClaimType;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipClaimType;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanClaimPayoutRepository extends JpaRepository<MembershipPlanClaimPayoutEntity, String> {

    @EntityGraph(attributePaths = "plan")
    List<MembershipPlanClaimPayoutEntity> findByPlanIdAndActiveTrue(String planId);

    @EntityGraph(attributePaths = "plan")
    List<MembershipPlanClaimPayoutEntity> findByPlanId(String planId);

    @EntityGraph(attributePaths = "plan")
    Optional<MembershipPlanClaimPayoutEntity> findByPlanIdAndId(String planId, String id);

    Optional<MembershipPlanClaimPayoutEntity> findByPlanIdAndClaimTypeAndDependentTypeAndActiveTrue(
            String planId,
            MembershipClaimType claimType,
            DependentType dependentType
    );

    boolean existsByPlanIdAndClaimTypeAndDependentType(
            String planId,
            MembershipClaimType claimType,
            DependentType dependentType
    );

    boolean existsByPlanIdAndClaimTypeAndDependentTypeAndIdNot(
            String planId,
            MembershipClaimType claimType,
            DependentType dependentType,
            String id
    );
}
