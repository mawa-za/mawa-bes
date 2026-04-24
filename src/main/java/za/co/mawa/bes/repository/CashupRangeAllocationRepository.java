package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.CashupRangeAllocationEntity;
import za.co.mawa.bes.entity.ReceiptRangeAllocationEntity;

import java.util.Optional;

public interface CashupRangeAllocationRepository extends JpaRepository<CashupRangeAllocationEntity, Long> {
    Optional<CashupRangeAllocationEntity> findByDeviceIdAndStatus(String deviceId, String status);
}
