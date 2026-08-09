package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.LeaveProfileEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface LeaveProfileRepository extends JpaRepository<LeaveProfileEntity, String> {
    Optional<LeaveProfileEntity> findByCodeIgnoreCase(String code);
    Optional<LeaveProfileEntity> findFirstByDefaultProfileTrueAndActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqual(LocalDate from, LocalDate to);
    List<LeaveProfileEntity> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
}
