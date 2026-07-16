package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneInstallationTeamEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneInstallationTeamRepository extends JpaRepository<TombstoneInstallationTeamEntity, String> {
    List<TombstoneInstallationTeamEntity> findByInstallationIdOrderByCreatedAtAsc(String installationId);
    void deleteByInstallationId(String installationId);
}
