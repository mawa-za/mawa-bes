package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.FuneralPackageEntity;

import java.util.List;

@Repository
public interface FuneralPackageRepository extends JpaRepository<FuneralPackageEntity, String> {
    List<FuneralPackageEntity> findByActiveTrue();
}
