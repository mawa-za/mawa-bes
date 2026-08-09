package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.EmploymentLeaveProfileAssignmentEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface EmploymentLeaveProfileAssignmentRepository extends JpaRepository<EmploymentLeaveProfileAssignmentEntity, String> {
    List<EmploymentLeaveProfileAssignmentEntity> findByEmploymentIdOrderByEffectiveFromDesc(String employmentId);
    Optional<EmploymentLeaveProfileAssignmentEntity> findFirstByEmploymentIdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(String employmentId, LocalDate from, LocalDate to);
}
