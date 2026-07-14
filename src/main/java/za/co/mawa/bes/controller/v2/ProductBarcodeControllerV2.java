package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.ProductBarcodeService;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2")
public class ProductBarcodeControllerV2 {
    private final ProductBarcodeService service;

    public ProductBarcodeControllerV2(ProductBarcodeService service) {
        this.service = service;
    }

    @GetMapping("/products/{productId}/barcodes")
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable String productId) {
        return ResponseEntity.ok(service.list(productId));
    }

    @PutMapping("/products/{productId}/barcodes")
    public ResponseEntity<List<Map<String, Object>>> replace(
            @PathVariable String productId,
            @RequestBody ProductBarcodeService.BarcodeReplaceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.replace(productId, request, userId));
    }

    @PostMapping("/products/{productId}/barcodes")
    public ResponseEntity<List<Map<String, Object>>> add(
            @PathVariable String productId,
            @RequestBody ProductBarcodeService.BarcodeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.add(productId, request, userId));
    }

    @DeleteMapping("/products/{productId}/barcodes/{barcodeId}")
    public ResponseEntity<Void> delete(@PathVariable String productId, @PathVariable String barcodeId) {
        service.delete(productId, barcodeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products/by-barcode/{barcode}")
    public ResponseEntity<Map<String, Object>> findProduct(@PathVariable String barcode) {
        return ResponseEntity.ok(service.findProduct(barcode));
    }
}
