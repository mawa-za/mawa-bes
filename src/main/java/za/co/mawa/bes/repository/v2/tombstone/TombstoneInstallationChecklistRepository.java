package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneInstallationChecklistEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneInstallationChecklistRepository extends JpaRepository<TombstoneInstallationChecklistEntity, String> {
    List<TombstoneInstallationChecklistEntity> findByInstallationIdOrderByCreatedAtAsc(String installationId);
    long countByInstallationIdAndRequiredTrueAndCompletedFalse(String installationId);
}
