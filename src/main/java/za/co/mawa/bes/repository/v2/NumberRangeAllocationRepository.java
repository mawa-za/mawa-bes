package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.NumberRangeAllocationEntity;

import java.util.List;
import java.util.Optional;

public interface NumberRangeAllocationRepository extends JpaRepository<NumberRangeAllocationEntity, Long> {

    List<NumberRangeAllocationEntity> findByDeviceIdAndSeqTypeOrderByIdDesc(
            String deviceId,
            String seqType
    );

    Optional<NumberRangeAllocationEntity> findFirstByDeviceIdAndSeqTypeAndStatusOrderByIdDesc(
            String deviceId,
            String seqType,
            String status
    );
}