package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneSiteAssessmentEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneSiteAssessmentRepository extends JpaRepository<TombstoneSiteAssessmentEntity, String> {
    List<TombstoneSiteAssessmentEntity> findByTombstoneOrderIdOrderByVersionNoDesc(String tombstoneOrderId);
    Optional<TombstoneSiteAssessmentEntity> findFirstByTombstoneOrderIdOrderByVersionNoDesc(String tombstoneOrderId);
    List<TombstoneSiteAssessmentEntity> findByStatusOrderByCreatedAtDesc(String status);
}
