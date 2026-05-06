package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;

import java.util.Optional;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlanEntity, String> {
    Optional<MembershipPlanEntity> findByOldId(String oldId);
}