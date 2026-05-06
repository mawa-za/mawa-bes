package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipEntity;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<MembershipEntity, String> {

    Optional<MembershipEntity> findByOldId(String oldId);
}