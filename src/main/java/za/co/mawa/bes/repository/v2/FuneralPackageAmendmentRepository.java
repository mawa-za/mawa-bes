package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import za.co.mawa.bes.entity.v2.FuneralPackageAmendmentEntity;
import java.util.List;
import java.util.Optional;

public interface FuneralPackageAmendmentRepository extends JpaRepository<FuneralPackageAmendmentEntity, String> {
    List<FuneralPackageAmendmentEntity> findByFuneralServiceIdOrderByCreatedAtDesc(String funeralServiceId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM FuneralPackageAmendmentEntity a WHERE a.id = :id")
    Optional<FuneralPackageAmendmentEntity> findWithLockById(@Param("id") String id);
}
