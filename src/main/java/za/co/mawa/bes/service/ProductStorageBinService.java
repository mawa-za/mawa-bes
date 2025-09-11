package za.co.mawa.bes.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.ProductStorageBinEntity;
import za.co.mawa.bes.entity.ProductStorageBinPKEntity;
import za.co.mawa.bes.repository.ProductStorageBinRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductStorageBinService {
//
//    private final ProductStorageBinRepository productStorageBinRepository;
//
//    public ProductStorageBinEntity assignProductToBin(ProductStorageBinEntity productStorageBin) {
//        return productStorageBinRepository.save(productStorageBin);
//    }
//
//    public List<ProductStorageBinEntity> getBinsForProduct(String productId) {
//        return productStorageBinRepository.findByIdProductId(productId);
//    }
//
//    public List<ProductStorageBinEntity> getProductsInBin(String binId) {
//        return productStorageBinRepository.findByIdBinId(binId);
//    }
//
//    public Optional<ProductStorageBinEntity> getAssignment(String binId, String productId) {
//        return productStorageBinRepository.findByBinIdAndProductId(binId, productId);
//    }
//
//    public void removeAssignment(String binId, String productId) {
//        ProductStorageBinPKEntity id = new ProductStorageBinPKEntity(binId, productId);
//        productStorageBinRepository.deleteById(id);
//    }
}
