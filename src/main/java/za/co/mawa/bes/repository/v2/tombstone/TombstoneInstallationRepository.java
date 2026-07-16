package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneInstallationEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneInstallationRepository extends JpaRepository<TombstoneInstallationEntity, String> {
    List<TombstoneInstallationEntity> findByTombstoneOrderIdOrderByCreatedAtDesc(String tombstoneOrderId);
    List<TombstoneInstallationEntity> findByStatusOrderByScheduledStartAtAsc(String status);
    List<TombstoneInstallationEntity> findAllByOrderByScheduledStartAtAsc();
}
