package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneDesignEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneDesignRepository extends JpaRepository<TombstoneDesignEntity, String> {
    List<TombstoneDesignEntity> findByTombstoneOrderIdOrderByVersionNoDesc(String tombstoneOrderId);
    Optional<TombstoneDesignEntity> findFirstByTombstoneOrderIdOrderByVersionNoDesc(String tombstoneOrderId);
    Optional<TombstoneDesignEntity> findFirstByTombstoneOrderIdAndStatusOrderByVersionNoDesc(String tombstoneOrderId, String status);
    List<TombstoneDesignEntity> findByStatusOrderByCreatedAtDesc(String status);
}
