package za.co.mawa.bes.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import za.co.mawa.bes.entity.CashupSequenceEntity;
import za.co.mawa.bes.entity.ReceiptSequenceEntity;

import java.util.Optional;

public interface CashupSequenceRepository extends JpaRepository<CashupSequenceEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CashupSequenceEntity s where s.id = 1")
    Optional<CashupSequenceEntity> findForUpdate();
}