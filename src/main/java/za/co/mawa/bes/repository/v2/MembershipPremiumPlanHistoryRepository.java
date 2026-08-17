package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipPremiumPlanHistoryEntity;

import java.util.List;
import java.util.Optional;

public interface MembershipPremiumPlanHistoryRepository extends JpaRepository<MembershipPremiumPlanHistoryEntity, String> {
    List<MembershipPremiumPlanHistoryEntity> findByMembershipIdOrderByEffectiveFromAsc(String membershipId);
    Optional<MembershipPremiumPlanHistoryEntity> findFirstByMembershipIdAndEffectiveToIsNullOrderByEffectiveFromDesc(String membershipId);
}
