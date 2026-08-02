package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.LeaveProfileRuleEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface LeaveProfileRuleRepository extends JpaRepository<LeaveProfileRuleEntity, String> {
    List<LeaveProfileRuleEntity> findByLeaveProfileIdOrderByCreatedAtAsc(String leaveProfileId);
    Optional<LeaveProfileRuleEntity> findFirstByLeaveProfileIdAndLeaveTypeIdAndActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqualOrderByActiveFromDesc(String profileId, String leaveTypeId, LocalDate from, LocalDate to);
    List<LeaveProfileRuleEntity> findByLeaveProfileIdAndActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqual(String profileId, LocalDate from, LocalDate to);
}
