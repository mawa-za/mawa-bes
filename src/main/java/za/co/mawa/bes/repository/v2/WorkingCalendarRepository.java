package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.WorkingCalendarEntity;
import java.util.List;
import java.util.Optional;
@Repository
public interface WorkingCalendarRepository extends JpaRepository<WorkingCalendarEntity, String> {
    List<WorkingCalendarEntity> findAllByOrderByNameAsc();
    Optional<WorkingCalendarEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
