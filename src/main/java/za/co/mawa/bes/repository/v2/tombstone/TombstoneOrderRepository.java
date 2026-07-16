package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneOrderEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneOrderRepository extends JpaRepository<TombstoneOrderEntity, String> {
    Optional<TombstoneOrderEntity> findByOrderNo(String orderNo);
    List<TombstoneOrderEntity> findAllByOrderByCreatedAtDesc();
    List<TombstoneOrderEntity> findByStatusOrderByCreatedAtDesc(String status);
    List<TombstoneOrderEntity> findByFundingStatusOrderByCreatedAtDesc(String fundingStatus);
}
