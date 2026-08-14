package za.co.mawa.bes.repository.v2;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsByMembershipIdAndDeceasedPartnerIdAndApprovedAtIsNotNull(
            String membershipId, String deceasedPartnerId);

    boolean existsByMembershipIdAndDeceasedPartnerIdAndStatusIn(
            String membershipId, String deceasedPartnerId, List<MembershipClaimStatus> statuses);

    @Query("""
            SELECT claim
              FROM MembershipClaimEntity claim
             WHERE (:status IS NULL OR claim.status = :status)
               AND (
                    :query IS NULL OR :query = ''
                    OR LOWER(claim.claimNo) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR EXISTS (
                        SELECT membership.id
                          FROM MembershipEntity membership
                         WHERE membership.id = claim.membershipId
                           AND LOWER(membership.membershipNo) LIKE LOWER(CONCAT('%', :query, '%'))
                    )
                    OR EXISTS (
                        SELECT deceased.partnerId
                          FROM PartnerViewEntity deceased
                         WHERE deceased.partnerId = claim.deceasedPartnerId
                           AND (
                                LOWER(COALESCE(deceased.partnerNo, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(COALESCE(deceased.identityNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(COALESCE(deceased.name1, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(COALESCE(deceased.name2, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(COALESCE(deceased.name3, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           )
                    )
                    OR EXISTS (
                        SELECT member.partnerId
                          FROM PartnerViewEntity member
                         WHERE member.partnerId IN (
                               SELECT membership.memberId
                                 FROM MembershipEntity membership
                                WHERE membership.id = claim.membershipId
                         )
                           AND (
                                LOWER(COALESCE(member.partnerNo, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(COALESCE(member.identityNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(COALESCE(member.name1, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(COALESCE(member.name2, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(COALESCE(member.name3, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                           )
                    )
               )
             ORDER BY claim.createdAt DESC
            """)
    Slice<MembershipClaimEntity> searchPage(
            @Param("status") MembershipClaimStatus status,
            @Param("query") String query,
            Pageable pageable
    );

    Slice<MembershipClaimEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Slice<MembershipClaimEntity> findByStatusOrderByCreatedAtDesc(
            MembershipClaimStatus status,
            Pageable pageable
    );
}

