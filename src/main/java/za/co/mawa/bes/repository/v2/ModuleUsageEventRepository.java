package za.co.mawa.bes.repository.v2;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.ModuleUsageEventEntity;

import java.util.List;

public interface ModuleUsageEventRepository extends JpaRepository<ModuleUsageEventEntity, String> {

    List<ModuleUsageEventEntity> findByUserIdOrderByUsedAtDesc(String userId, Pageable pageable);

    void deleteByUserId(String userId);
}
