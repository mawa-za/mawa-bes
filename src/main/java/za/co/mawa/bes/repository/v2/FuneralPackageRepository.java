package za.co.mawa.bes.repository.v2;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.FuneralPackageEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuneralPackageRepository extends JpaRepository<FuneralPackageEntity, String> {
    List<FuneralPackageEntity> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select funeralPackage from FuneralPackageEntity funeralPackage where funeralPackage.id = :id")
    Optional<FuneralPackageEntity> findByIdForUpdate(@Param("id") String id);
}
