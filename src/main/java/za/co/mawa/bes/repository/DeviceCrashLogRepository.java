package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import za.co.mawa.bes.entity.DeviceCrashLogEntity;

import java.util.Optional;

public interface DeviceCrashLogRepository extends JpaRepository<DeviceCrashLogEntity, Long>, JpaSpecificationExecutor<DeviceCrashLogEntity> {
    Optional<DeviceCrashLogEntity> findByLogId(String logId);
}
