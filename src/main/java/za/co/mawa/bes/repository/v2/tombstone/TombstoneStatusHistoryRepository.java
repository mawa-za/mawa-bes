package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneStatusHistoryEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneStatusHistoryRepository extends JpaRepository<TombstoneStatusHistoryEntity, String> {
    List<TombstoneStatusHistoryEntity> findByTombstoneOrderIdOrderByChangedAtDesc(String tombstoneOrderId);
}
