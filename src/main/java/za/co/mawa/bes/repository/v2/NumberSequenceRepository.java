package za.co.mawa.bes.repository.v2;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.v2.NumberSequenceEntity;

import java.util.Optional;

public interface NumberSequenceRepository extends JpaRepository<NumberSequenceEntity, Long> {

    Optional<NumberSequenceEntity> findBySeqType(String seqType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NumberSequenceEntity n WHERE n.seqType = :seqType")
    Optional<NumberSequenceEntity> findBySeqTypeForUpdate(@Param("seqType") String seqType);
}