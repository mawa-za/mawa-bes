package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneFundingAllocationEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneFundingAllocationRepository extends JpaRepository<TombstoneFundingAllocationEntity, String> {
    List<TombstoneFundingAllocationEntity> findByTombstoneOrderIdOrderByCreatedAtAsc(String tombstoneOrderId);
    List<TombstoneFundingAllocationEntity> findBySourceTypeAndSourceId(String sourceType, String sourceId);
    Optional<TombstoneFundingAllocationEntity> findByTombstoneOrderIdAndSourceTypeAndSourceId(String tombstoneOrderId, String sourceType, String sourceId);
}
