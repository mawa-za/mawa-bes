package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import za.co.mawa.bes.entity.v2.MembershipChangeRequestEntity;
import za.co.mawa.bes.enums.MembershipChangeStatus;
import za.co.mawa.bes.enums.MembershipChangeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
public interface MembershipChangeRequestRepository extends JpaRepository<MembershipChangeRequestEntity, String> {
    List<MembershipChangeRequestEntity> findByMembershipIdOrderByRequestedAtDesc(String membershipId);
    boolean existsByMembershipIdAndStatusIn(String membershipId, Collection<MembershipChangeStatus> statuses);
    boolean existsByMembershipIdAndChangeTypeInAndStatusIn(
            String membershipId,
            Collection<MembershipChangeType> changeTypes,
            Collection<MembershipChangeStatus> statuses
    );
    boolean existsByMembershipIdAndNewDependentPartnerIdAndChangeTypeInAndStatusIn(
            String membershipId,
            String newDependentPartnerId,
            Collection<MembershipChangeType> changeTypes,
            Collection<MembershipChangeStatus> statuses
    );
    boolean existsByMembershipIdAndOldDependentIdAndChangeTypeInAndStatusIn(
            String membershipId,
            String oldDependentId,
            Collection<MembershipChangeType> changeTypes,
            Collection<MembershipChangeStatus> statuses
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      select r from MembershipChangeRequestEntity r
       where r.membershipId = :membershipId
         and r.status = :status
         and r.effectiveDate <= :effectiveDate
       order by r.effectiveDate, r.requestedAt
    """)
    List<MembershipChangeRequestEntity> findDueForMembershipForUpdate(
            @Param("membershipId") String membershipId,
            @Param("status") MembershipChangeStatus status,
            @Param("effectiveDate") LocalDate effectiveDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      select r from MembershipChangeRequestEntity r
       where r.status = :status
         and r.effectiveDate <= :effectiveDate
       order by r.effectiveDate, r.requestedAt
    """)
    List<MembershipChangeRequestEntity> findDueForUpdate(
            @Param("status") MembershipChangeStatus status,
            @Param("effectiveDate") LocalDate effectiveDate);
}
