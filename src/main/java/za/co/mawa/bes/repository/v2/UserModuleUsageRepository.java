package za.co.mawa.bes.repository.v2;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.UserModuleUsageEntity;

import java.util.List;
import java.util.Optional;

public interface UserModuleUsageRepository extends JpaRepository<UserModuleUsageEntity, String> {

    Optional<UserModuleUsageEntity> findByUserIdAndModuleCode(String userId, String moduleCode);

    List<UserModuleUsageEntity> findByUserIdOrderByUsageCountDescLastUsedAtDesc(String userId, Pageable pageable);

    List<UserModuleUsageEntity> findByUserIdOrderByLastUsedAtDesc(String userId, Pageable pageable);

    void deleteByUserId(String userId);
}
