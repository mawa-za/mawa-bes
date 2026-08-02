package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.EmployeeLeaveBalanceEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalanceEntity, String> {
    List<EmployeeLeaveBalanceEntity> findByEmploymentIdOrderByCycleStartDesc(String employmentId);
    Optional<EmployeeLeaveBalanceEntity> findFirstByEmploymentIdAndLeaveTypeIdAndCycleStartLessThanEqualAndCycleEndGreaterThanEqual(String employmentId, String leaveTypeId, LocalDate from, LocalDate to);
    Optional<EmployeeLeaveBalanceEntity> findFirstByEmploymentIdAndLeaveTypeIdAndCycleEndBeforeOrderByCycleEndDesc(String employmentId, String leaveTypeId, LocalDate cycleStart);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from EmployeeLeaveBalanceEntity b where b.id = :id")
    Optional<EmployeeLeaveBalanceEntity> findByIdForUpdate(@Param("id") String id);
}
