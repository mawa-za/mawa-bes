package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;

import java.util.List;

public interface MembershipDependentRepository extends JpaRepository<MembershipDependentEntity, String> {
    List<MembershipDependentEntity> findByMembershipId(String membershipId);
    boolean existsByMembershipIdAndDependentPartnerId(String membershipId, String dependentPartnerId);
}