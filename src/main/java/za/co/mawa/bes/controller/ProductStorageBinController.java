package za.co.mawa.bes.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.entity.ProductStorageBinEntity;
import za.co.mawa.bes.service.ProductStorageBinService;

import java.util.List;
import za.co.mawa.bes.dto.ProductStorageBinCreateRequestDto;
import za.co.mawa.bes.dto.ProductStorageBinResponseDto;
import za.co.mawa.bes.dto.ProductStorageBinUpdateRequestDto;
import za.co.mawa.bes.mapper.ProductStorageBinMapper;


@RestController
@RequestMapping("product-storage-bin")
@RequiredArgsConstructor
public class ProductStorageBinController {

    private final ProductStorageBinMapper productStorageBinMapper;

//    @Autowired
//    ProductStorageBinService productStorageBinService;
//
//    @PostMapping
//    public ResponseEntity<ProductStorageBinResponseDto> assignProduct(@RequestBody ProductStorageBinCreateRequestDto productStorageBin) {
//        return ResponseEntity.ok(productStorageBinService.assignProductToBin(productStorageBin));
//    }
//
//    @GetMapping("/product/{productId}")
//    public ResponseEntity<List<ProductStorageBinResponseDto>> getBinsForProduct(@PathVariable String productId) {
//        return ResponseEntity.ok(productStorageBinService.getBinsForProduct(productId));
//    }
//
//    @GetMapping("/bin/{binCode}")
//    public ResponseEntity<List<ProductStorageBinResponseDto>> getProductsInBin(@PathVariable String binCode) {
//        return ResponseEntity.ok(productStorageBinService.getProductsInBin(binCode));
//    }
//
//    @GetMapping("/{binCode}/{productId}")
//    public ResponseEntity<ProductStorageBinResponseDto> getAssignment(
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
