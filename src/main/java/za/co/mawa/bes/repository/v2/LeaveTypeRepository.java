package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.LeaveTypeEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveTypeEntity, String> {
    Optional<LeaveTypeEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<LeaveTypeEntity> findAllByOrderByDisplayOrderAscNameAsc();
    List<LeaveTypeEntity> findByActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqualOrderByDisplayOrderAscNameAsc(LocalDate from, LocalDate to);
}
