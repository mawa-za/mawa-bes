package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneProductionJobEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneProductionJobRepository extends JpaRepository<TombstoneProductionJobEntity, String> {
    List<TombstoneProductionJobEntity> findByTombstoneOrderIdOrderByCreatedAtDesc(String tombstoneOrderId);
    Optional<TombstoneProductionJobEntity> findFirstByTombstoneOrderIdOrderByCreatedAtDesc(String tombstoneOrderId);
    List<TombstoneProductionJobEntity> findByStatusOrderByCreatedAtDesc(String status);
}
