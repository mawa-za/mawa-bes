package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.ProductStorageBinEntity;
import za.co.mawa.bes.entity.ProductStorageBinPKEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductStorageBinRepository extends JpaRepository<ProductStorageBinEntity, ProductStorageBinPKEntity> {

//    // Find all bins for a product
//    List<ProductStorageBinEntity> findByIdProductId(String productId);
//
//    // Find all products assigned to a bin
//    List<ProductStorageBinEntity> findByIdBinId(String binId);
//
//    // Find by bin and product
//    Optional<ProductStorageBinEntity> findByBinIdAndProductId(String binId, String productId);
}
