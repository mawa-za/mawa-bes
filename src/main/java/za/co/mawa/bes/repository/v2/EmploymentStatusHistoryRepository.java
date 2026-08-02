package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.EmploymentStatusHistoryEntity;
import java.util.List;
@Repository
public interface EmploymentStatusHistoryRepository extends JpaRepository<EmploymentStatusHistoryEntity, String> {
    List<EmploymentStatusHistoryEntity> findByEmploymentIdOrderByEffectiveDateDescChangedAtDesc(String employmentId);
    List<EmploymentStatusHistoryEntity> findAllByOrderByChangedAtDesc();
}
