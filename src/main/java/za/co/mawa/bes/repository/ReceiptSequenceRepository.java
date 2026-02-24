package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import za.co.mawa.bes.entity.ReceiptSequenceEntity;

import java.util.Optional;

public interface ReceiptSequenceRepository extends JpaRepository<ReceiptSequenceEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ReceiptSequenceEntity s where s.id = 1")
    Optional<ReceiptSequenceEntity> findForUpdate();
}