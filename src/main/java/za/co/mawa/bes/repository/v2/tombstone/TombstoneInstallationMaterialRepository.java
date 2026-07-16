package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneInstallationMaterialEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneInstallationMaterialRepository extends JpaRepository<TombstoneInstallationMaterialEntity, String> {
    List<TombstoneInstallationMaterialEntity> findByInstallationIdOrderByCreatedAtAsc(String installationId);
    void deleteByInstallationId(String installationId);
}
