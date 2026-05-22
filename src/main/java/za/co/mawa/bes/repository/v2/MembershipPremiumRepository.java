package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.enums.PremiumStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MembershipPremiumRepository extends JpaRepository<MembershipPremiumEntity, String> {

    Optional<MembershipPremiumEntity> findByMembershipIdAndPeriodYYYYMM(
            String membershipId,
            String periodYYYYMM
    );

    List<MembershipPremiumEntity> findByMembershipIdOrderByPeriodYYYYMMAsc(String membershipId);

    List<MembershipPremiumEntity> findByMembershipIdAndStatusInOrderByPeriodYYYYMMAsc(
            String membershipId,
            Collection<PremiumStatus> statuses
    );

    boolean existsByMembershipIdAndPeriodYYYYMM(String membershipId, String periodYYYYMM);
}
