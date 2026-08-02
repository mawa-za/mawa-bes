package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.EmployeeNumberAssignmentEntity;
import java.util.Optional;
@Repository
public interface EmployeeNumberAssignmentRepository extends JpaRepository<EmployeeNumberAssignmentEntity, String> {
    Optional<EmployeeNumberAssignmentEntity> findByPartnerId(String partnerId);
}
