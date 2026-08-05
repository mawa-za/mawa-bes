package za.co.mawa.bes.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.DeviceSyncSubmissionEntity;
import java.util.Optional;
public interface DeviceSyncSubmissionRepository extends JpaRepository<DeviceSyncSubmissionEntity, Long> {
 Optional<DeviceSyncSubmissionEntity> findBySubmissionId(String submissionId);
 Optional<DeviceSyncSubmissionEntity> findByIdempotencyKey(String idempotencyKey);
}
