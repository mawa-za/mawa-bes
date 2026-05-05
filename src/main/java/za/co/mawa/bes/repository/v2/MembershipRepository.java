package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipEntity;

public interface MembershipRepository extends JpaRepository<MembershipEntity, String> {
    // Default JPA methods
}