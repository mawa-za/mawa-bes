package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.v2.MembershipPlanHistoryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface MembershipPlanHistoryRepository extends JpaRepository<MembershipPlanHistoryEntity, String> {
    List<MembershipPlanHistoryEntity> findByMembershipIdOrderByEffectiveFromAsc(String membershipId);
    Optional<MembershipPlanHistoryEntity> findFirstByMembershipIdAndEffectiveToIsNullOrderByEffectiveFromDesc(String membershipId);
    @Query("""
      select h from MembershipPlanHistoryEntity h
       where h.membershipId = :membershipId
         and h.effectiveFrom <= :date
         and (h.effectiveTo is null or h.effectiveTo >= :date)
       order by h.effectiveFrom desc
    """)
    List<MembershipPlanHistoryEntity> findEffective(@Param("membershipId") String membershipId, @Param("date") LocalDate date);
}
