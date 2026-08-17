package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.SystemInstallationFileEntity;

import java.util.List;

public interface SystemInstallationFileRepository extends JpaRepository<SystemInstallationFileEntity, String> {
    List<SystemInstallationFileEntity> findByActiveTrueOrderByDisplayNameAsc();
}
