package za.co.mawa.bes.repository.v2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import za.co.mawa.bes.entity.v2.MembershipEntity;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<MembershipEntity, String> , JpaSpecificationExecutor<MembershipEntity>
{

    Optional<MembershipEntity> findByOldId(String oldId);
    Page<MembershipEntity> findByMemberId(String memberId, Pageable pageable);
}