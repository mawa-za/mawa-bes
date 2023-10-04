package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.ProductUpdateDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeCreateDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeEditDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.entity.ProductAttributePKEntity;
import za.co.mawa.bes.exception.ProductNotFoundException;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.utils.PriceType;

@RestController
@CrossOrigin
public class ProductController {
    @Autowired
    ProductService productService;
    Gson gson = new Gson();

    @RequestMapping(value = "/product", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postProduct(@RequestBody ProductCreateDto productCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.create(productCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/product", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProducts(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String category) {
        try {
            ProductQueryDto productQueryDto = new ProductQueryDto();
            if(code != null && code != "") {
                productQueryDto.setCode(code);
            }
            if(category != null && category != "") {
                productQueryDto.setCategory(category);
            }
            return ResponseEntity.ok(gson.toJson(productService.search(productQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/product/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.get(id)));
        } catch (ProductNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @RequestMapping(value = "/product/{id}", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editProduct(@PathVariable String id, @RequestBody ProductUpdateDto productUpdateDto) {
        try {
            ProductDto productDto = productService.get(id);
            if(productUpdateDto.getCode() != null && productUpdateDto.getCode() != ""){
                productDto.setCode(productUpdateDto.getCode());
            }
            if(productUpdateDto.getCategory() != null && productUpdateDto.getCategory() != ""){
                productDto.setCategory(productUpdateDto.getCategory());
            }
            if(productUpdateDto.getDescription() != null && productUpdateDto.getDescription() != ""){
                productDto.setDescription(productUpdateDto.getDescription());
            }
            if(productUpdateDto.getCode() != null && productUpdateDto.getCode() != ""){
                productDto.setCode(productUpdateDto.getCode());
            }
            if(productUpdateDto.getBaseUnitOfMeasure() != null && productUpdateDto.getBaseUnitOfMeasure() != "") {
                productDto.setBaseUnitOfMeasure(productUpdateDto.getBaseUnitOfMeasure());
            }
            productService.edit(productDto);

          if(productUpdateDto.getPrice() != null){
            ProductPricingDto productPricingDto = new ProductPricingDto();
            productPricingDto.setProduct(id);
            if(productUpdateDto.getPricingType() == null || productUpdateDto.getPricingType() == "") {
                productPricingDto.setPricing(PriceType.SELLING_PRICE);
            }else{
                productPricingDto.setPricing(productUpdateDto.getPricingType());
            }

            productPricingDto.setValue(productUpdateDto.getPrice());
            productService.editPricing(productPricingDto);
          }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "/product/{id}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/product/{id}/attribute", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addAttribute(@PathVariable String id, @RequestBody ProductAttributeCreateDto attributeCreate) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.addAttribute(attributeCreate)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/product/{id}/attribute", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAttribute(@PathVariable String id) {
        try {
            ProductAttributeQueryDto queryDto = new ProductAttributeQueryDto();
            queryDto.setProduct(id);
            return ResponseEntity.ok(gson.toJson(productService.getAttributes(queryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/product/{id}/attribute", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteAttribute(@PathVariable String id,@RequestParam String attribute) {
        try {
            ProductAttributePKEntity pk = new ProductAttributePKEntity();
            pk.setProduct(id);
            pk.setAttribute(attribute);
            return ResponseEntity.ok(gson.toJson(productService.deleteAttribute(pk)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/product/{id}/attribute", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editAttribute(@PathVariable String id,
                                           @RequestParam String attribute,
                                           @RequestBody ProductAttributeEditDto editDto) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.editAttribute(editDto,id,attribute)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

}
