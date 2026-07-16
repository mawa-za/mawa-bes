package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneOrderAmendmentEntity;
import java.util.List;

public interface TombstoneOrderAmendmentRepository extends JpaRepository<TombstoneOrderAmendmentEntity, String> {
    List<TombstoneOrderAmendmentEntity> findByTombstoneOrderIdOrderByAmendmentNoDesc(String tombstoneOrderId);
    long countByTombstoneOrderIdAndStatus(String tombstoneOrderId, String status);
}
