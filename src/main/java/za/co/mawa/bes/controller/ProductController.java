package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.ProductUpdateDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.exception.ProductNotFound;
import za.co.mawa.bes.service.ProductService;

@RestController
@CrossOrigin
public class ProductController {
    @Autowired
    ProductService productService;
    Gson gson = new Gson();

    @RequestMapping(value = "/product", method = RequestMethod.POST)
    public ResponseEntity<?> postProduct(@RequestBody ProductCreateDto productCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.create(productCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/product", method = RequestMethod.GET)
    public ResponseEntity<?> getProducts(@RequestBody ProductQueryDto productQueryDto) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.search(productQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/product/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.get(id)));
        } catch (ProductNotFound exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @RequestMapping(value = "/product/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editProduct(@PathVariable String id, @RequestBody ProductUpdateDto productUpdateDto) {
        try {
            ProductDto productDto = productService.get(id);
            productDto.setCode(productUpdateDto.getCode());
            productDto.setCategory(productUpdateDto.getCategory());
            productDto.setDescription(productUpdateDto.getDescription());
//            productDto.setBaseUnitOfMeasure(productUpdateDto.getBaseUnitOfMeasure());
            productService.edit(productDto);
//
//
//            ProductPricingDto productPricingDto = new ProductPricingDto();
//            productPricingDto.setProduct(productEntity.getId());
//            productPricingDto.setPricing("SELLING-PRICE");
//            productPricingDto.setValue(productUpdateDto.getSellingPrice());
//            editPricing(productPricingDto);

            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "/product/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
