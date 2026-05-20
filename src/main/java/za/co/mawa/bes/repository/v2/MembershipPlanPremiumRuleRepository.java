package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.v2.MembershipPlanPremiumRuleEntity;
import za.co.mawa.bes.enums.DependentType;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanPremiumRuleRepository
        extends JpaRepository<MembershipPlanPremiumRuleEntity, String> {

    List<MembershipPlanPremiumRuleEntity> findByPlanIdAndActiveTrue(String planId);

    Optional<MembershipPlanPremiumRuleEntity>
    findFirstByPlanIdAndDependentTypeAndMinAgeLessThanEqualAndMaxAgeGreaterThanEqualAndActiveTrue(
            String planId,
            DependentType dependentType,
            Integer ageForMin,
            Integer ageForMax
    );

    @Query("""
        SELECT r
        FROM MembershipPlanPremiumRuleEntity r
        WHERE r.plan.id = :planId
          AND r.dependentType = :dependentType
          AND r.active = true
          AND (
                (:minAge BETWEEN r.minAge AND r.maxAge)
             OR (:maxAge BETWEEN r.minAge AND r.maxAge)
             OR (r.minAge BETWEEN :minAge AND :maxAge)
             OR (r.maxAge BETWEEN :minAge AND :maxAge)
          )
    """)
    List<MembershipPlanPremiumRuleEntity> findOverlappingRules(
            String planId,
            DependentType dependentType,
            Integer minAge,
            Integer maxAge
    );
}