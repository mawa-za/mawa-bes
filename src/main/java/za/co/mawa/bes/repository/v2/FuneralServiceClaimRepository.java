package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.FuneralServiceClaimEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuneralServiceClaimRepository extends JpaRepository<FuneralServiceClaimEntity, String> {
    List<FuneralServiceClaimEntity> findByFuneralServiceId(String funeralServiceId);
    Optional<FuneralServiceClaimEntity> findByMembershipClaimId(String membershipClaimId);
}
