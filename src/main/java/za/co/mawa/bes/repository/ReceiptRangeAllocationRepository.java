package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.ReceiptRangeAllocationEntity;

import java.util.Optional;

public interface ReceiptRangeAllocationRepository extends JpaRepository<ReceiptRangeAllocationEntity, Long> {
    Optional<ReceiptRangeAllocationEntity> findByDeviceIdAndStatus(String deviceId, String status);
}
