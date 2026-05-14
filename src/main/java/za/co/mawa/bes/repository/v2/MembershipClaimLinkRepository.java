package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipClaimLinkEntity;

import java.util.List;

public interface MembershipClaimLinkRepository extends JpaRepository<MembershipClaimLinkEntity, String> {

    List<MembershipClaimLinkEntity> findByParentClaimIdOrderByCreatedAtAsc(String parentClaimId);

    List<MembershipClaimLinkEntity> findByLinkedClaimIdOrderByCreatedAtAsc(String linkedClaimId);

    boolean existsByParentClaimId(String parentClaimId);

    boolean existsByLinkedClaimId(String linkedClaimId);

    boolean existsByParentClaimIdAndLinkedClaimId(String parentClaimId, String linkedClaimId);

    void deleteByParentClaimIdAndLinkedClaimId(String parentClaimId, String linkedClaimId);

    void deleteByParentClaimId(String parentClaimId);
}
