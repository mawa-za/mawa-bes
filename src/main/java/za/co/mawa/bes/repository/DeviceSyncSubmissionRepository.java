package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.DeviceSyncSubmissionEntity;

import java.util.Optional;

public interface DeviceSyncSubmissionRepository extends JpaRepository<DeviceSyncSubmissionEntity, Long>, JpaSpecificationExecutor<DeviceSyncSubmissionEntity> {
    Optional<DeviceSyncSubmissionEntity> findBySubmissionId(String submissionId);

    // Serialize competing /process and /reprocess requests for the same operation.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DeviceSyncSubmissionEntity s where s.submissionId = :submissionId")
    Optional<DeviceSyncSubmissionEntity> findBySubmissionIdForUpdate(@Param("submissionId") String submissionId);
    Optional<DeviceSyncSubmissionEntity> findByIdempotencyKey(String idempotencyKey);
}
