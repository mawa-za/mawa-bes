package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.FuneralExternalMembershipCoverEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface FuneralExternalMembershipCoverRepository extends JpaRepository<FuneralExternalMembershipCoverEntity, String> {
    List<FuneralExternalMembershipCoverEntity> findByIdentityNumberAndStatus(String identityNumber, String status);
    List<FuneralExternalMembershipCoverEntity> findByIdInAndStatus(Collection<String> ids, String status);
}
