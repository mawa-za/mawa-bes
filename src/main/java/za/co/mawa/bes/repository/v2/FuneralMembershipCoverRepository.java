package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.FuneralMembershipCoverEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface FuneralMembershipCoverRepository extends JpaRepository<FuneralMembershipCoverEntity, String> {
    List<FuneralMembershipCoverEntity> findByIdentityNumberAndStatus(String identityNumber, String status);
    List<FuneralMembershipCoverEntity> findByMembershipIdInAndStatus(Collection<String> membershipIds, String status);
}
