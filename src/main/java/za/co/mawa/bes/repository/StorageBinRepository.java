package za.co.mawa.bes.repository;

import za.co.mawa.bes.entity.StorageBinEntity;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface StorageBinRepository extends JpaRepository<StorageBinEntity, String> {

    // Find by binCode
    Optional<StorageBinEntity> findByBinCode(String binCode);

    // Find all bins in a warehouse
    List<StorageBinEntity> findByWarehouseId(String warehouseId);


    // Find by status (e.g., AVAILABLE, BLOCKED)
    List<StorageBinEntity> findByStatus(String status);
}

