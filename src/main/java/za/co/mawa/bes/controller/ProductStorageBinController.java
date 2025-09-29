package za.co.mawa.bes.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.entity.ProductStorageBinEntity;
import za.co.mawa.bes.service.ProductStorageBinService;

import java.util.List;

@RestController
@RequestMapping("product-storage-bin")
@RequiredArgsConstructor
public class ProductStorageBinController {

//    @Autowired
//    ProductStorageBinService productStorageBinService;
//
//    @PostMapping
//    public ResponseEntity<ProductStorageBinEntity> assignProduct(@RequestBody ProductStorageBinEntity productStorageBin) {
//        return ResponseEntity.ok(productStorageBinService.assignProductToBin(productStorageBin));
//    }
//
//    @GetMapping("/product/{productId}")
//    public ResponseEntity<List<ProductStorageBinEntity>> getBinsForProduct(@PathVariable String productId) {
//        return ResponseEntity.ok(productStorageBinService.getBinsForProduct(productId));
//    }
//
//    @GetMapping("/bin/{binCode}")
//    public ResponseEntity<List<ProductStorageBinEntity>> getProductsInBin(@PathVariable String binCode) {
//        return ResponseEntity.ok(productStorageBinService.getProductsInBin(binCode));
//    }
//
//    @GetMapping("/{binCode}/{productId}")
//    public ResponseEntity<ProductStorageBinEntity> getAssignment(
//            @PathVariable String binCode,
//            @PathVariable String productId) {
//        return productStorageBinService.getAssignment(binCode, productId)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @DeleteMapping("/{binCode}/{productId}")
//    public ResponseEntity<Void> removeAssignment(
//            @PathVariable String binCode,
//            @PathVariable String productId) {
//        productStorageBinService.removeAssignment(binCode, productId);
//        return ResponseEntity.noContent().build();
//    }
}
