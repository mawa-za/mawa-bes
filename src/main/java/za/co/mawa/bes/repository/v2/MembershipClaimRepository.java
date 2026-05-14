package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;

import java.util.List;
import java.util.Optional;

public interface MembershipClaimRepository extends JpaRepository<MembershipClaimEntity, String> {

    Optional<MembershipClaimEntity> findByClaimNo(String claimNo);

    List<MembershipClaimEntity> findByMembershipIdOrderByCreatedAtDesc(String membershipId);

    List<MembershipClaimEntity> findByStatusOrderByCreatedAtDesc(MembershipClaimStatus status);

    List<MembershipClaimEntity> findByClaimTypeOrderByCreatedAtDesc(MembershipClaimType claimType);

    List<MembershipClaimEntity> findByDeceasedPartnerIdOrderByCreatedAtDesc(String deceasedPartnerId);
}
