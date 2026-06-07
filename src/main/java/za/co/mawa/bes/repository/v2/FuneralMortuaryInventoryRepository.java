package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.FuneralMortuaryInventoryEntity;

import java.util.List;

@Repository
public interface FuneralMortuaryInventoryRepository extends JpaRepository<FuneralMortuaryInventoryEntity, String> {
    List<FuneralMortuaryInventoryEntity> findByStatus(String status);
}
