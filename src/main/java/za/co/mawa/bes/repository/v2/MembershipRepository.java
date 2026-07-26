package za.co.mawa.bes.repository.v2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import za.co.mawa.bes.entity.v2.MembershipEntity;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<MembershipEntity, String> , JpaSpecificationExecutor<MembershipEntity>
{

    Optional<MembershipEntity> findByOldId(String oldId);
    Optional<MembershipEntity> findByMembershipNo(String membershipNo);
    boolean existsByMemberId(String memberId);
    long countByMemberId(String memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MembershipEntity m where m.id = :id")
    Optional<MembershipEntity> findByIdForUpdate(@Param("id") String id);
    Page<MembershipEntity> findByMemberId(String memberId, Pageable pageable);

    /**
     * Lightweight, paged master-data feed for MAWA Pay.
     *
     * Returning a bounded list avoids the previous unbounded partner_view response
     * which could keep a Cloud Run request busy long after the device timed out.
     * The preferred identity lookup is backed by idx_partner_identity_partner.
     */
    @Query(value = """
            SELECT
                m.id AS membershipId,
                m.membership_no AS membershipNo,
                m.member_id AS partnerId,
                m.plan_id AS planId,
                m.premium_cents AS premiumCents,
                m.start_date AS startDate,
                m.join_date AS joinDate,
                m.status AS membershipStatus,
                COALESCE(
                    NULLIF(m.paid_up_to_period, ''),
                    (SELECT MAX(mp.period_yyyymm)
                       FROM membership_premium mp
                      WHERE (mp.membership_id = m.id OR mp.membership_id = m.old_id)
                        AND mp.status = 'PAID')
                ) AS paidUpToPeriod,
                m.created_at AS createdAt,
                m.updated_at AS updatedAt,
                p.number AS partnerNo,
                p.type AS partnerType,
                p.name1 AS name1,
                p.name2 AS name2,
                p.name3 AS name3,
                COALESCE(
                    (SELECT pi.type
                       FROM partner_identity pi
                      WHERE pi.partner = p.id
                      ORDER BY CASE WHEN pi.type = 'SA-ID' THEN 0 ELSE 1 END, pi.type, pi.value
                      LIMIT 1),
                    'SA-ID'
                ) AS identityType,
                COALESCE(
                    (SELECT pi.value
                       FROM partner_identity pi
                      WHERE pi.partner = p.id
                      ORDER BY CASE WHEN pi.type = 'SA-ID' THEN 0 ELSE 1 END, pi.type, pi.value
                      LIMIT 1),
                    p.number,
                    p.id
                ) AS identityNumber,
                p.birth_date AS birthDate,
                p.gender AS gender,
                p.status AS partnerStatus
            FROM membership m
            INNER JOIN partner p ON p.id = m.member_id
            ORDER BY m.id
            """, nativeQuery = true)
    List<MembershipMasterDataProjection> findMasterData(Pageable pageable);
}