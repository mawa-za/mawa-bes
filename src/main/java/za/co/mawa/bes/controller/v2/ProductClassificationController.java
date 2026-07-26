package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.product.classification.ProductCategoryCreateRequestDto;
import za.co.mawa.bes.dto.product.classification.ProductCategoryDefinitionDto;
import za.co.mawa.bes.dto.product.classification.ProductCategoryUpdateRequestDto;
import za.co.mawa.bes.dto.product.classification.ProductTypeDefinitionDto;
import za.co.mawa.bes.service.v2.ProductClassificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v2")
public class ProductClassificationController {
    private final ProductClassificationService service;

    public ProductClassificationController(ProductClassificationService service) {
        this.service = service;
    }

    @GetMapping("/product-types")
    public List<ProductTypeDefinitionDto> productTypes() {
        return service.getProductTypes();
    }

    @GetMapping("/product-categories")
    public List<ProductCategoryDefinitionDto> categories(
            @RequestParam(defaultValue = "true") Boolean activeOnly,
            @RequestParam(required = false) String productType) {
        return service.getCategories(activeOnly, productType);
    }

    @GetMapping("/product-categories/{id}")
    public ProductCategoryDefinitionDto category(@PathVariable String id) {
        return service.getCategory(id);
    }

    @PostMapping("/product-categories")
    public ResponseEntity<ProductCategoryDefinitionDto> create(
            @RequestBody ProductCategoryCreateRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(service.create(request, user(authentication)));
    }

    @PutMapping("/product-categories/{id}")
    public ProductCategoryDefinitionDto update(
            @PathVariable String id,
            @RequestBody ProductCategoryUpdateRequestDto request,
            Authentication authentication) {
        return service.update(id, request, user(authentication));
    }

    @DeleteMapping("/product-categories/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id, Authentication authentication) {
        service.deactivate(id, user(authentication));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    private String user(Authentication authentication) {
        return authentication == null ? "SYSTEM" : authentication.getName();
    }
}
