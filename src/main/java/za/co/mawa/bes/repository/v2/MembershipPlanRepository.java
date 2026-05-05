package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlanEntity, String> {
    // Spring Data JPA handles CRUD automatically
}