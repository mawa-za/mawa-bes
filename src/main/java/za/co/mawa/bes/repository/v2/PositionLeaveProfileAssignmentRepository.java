package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.PositionLeaveProfileAssignmentEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface PositionLeaveProfileAssignmentRepository extends JpaRepository<PositionLeaveProfileAssignmentEntity, String> {
    List<PositionLeaveProfileAssignmentEntity> findAllByOrderByPositionCodeAscEffectiveFromDesc();
    Optional<PositionLeaveProfileAssignmentEntity> findFirstByPositionCodeIgnoreCaseAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(String positionCode, LocalDate from, LocalDate to);
}
