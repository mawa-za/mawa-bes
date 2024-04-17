package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.product.*;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeCreateDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeEditDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryCreateDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryProcessDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingCreateDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingEditDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.entity.ProductAttributePKEntity;
import za.co.mawa.bes.entity.ProductPricingPKEntity;
import za.co.mawa.bes.exception.ProductNotFoundException;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.utils.PriceType;

import java.math.BigDecimal;

@RestController
@CrossOrigin
@RequestMapping(value = "product")
public class ProductController {
    @Autowired
    ProductService productService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postProduct(@RequestBody ProductCreateDto productCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.create(productCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProducts(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String type) {
        try {
            ProductQueryDto productQueryDto = new ProductQueryDto();
            if (code != null && code != "") {
                productQueryDto.setCode(code);
            }
            if (category != null && category != "") {
                productQueryDto.setCategory(category);
            }
            if (type != null && type != "") {
                productQueryDto.setType(type);
            }
            return ResponseEntity.ok(gson.toJson(productService.search(productQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.get(id)));
        } catch (ProductNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editProduct(@PathVariable String id, @RequestBody ProductEditDto productEditDto) {
        try {
            productService.edit(productEditDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addAttribute(@PathVariable String id, @RequestBody ProductAttributeCreateDto attributeCreate) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.addAttribute(attributeCreate)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAttribute(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.getAttributes(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteAttribute(@PathVariable String id, @RequestParam String attribute) {
        try {
            ProductAttributePKEntity pk = new ProductAttributePKEntity();
            pk.setProduct(id);
            pk.setAttribute(attribute);
            return ResponseEntity.ok(gson.toJson(productService.deleteAttribute(pk)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editAttribute(@PathVariable String id,
                                           @RequestParam String attribute,
                                           @RequestBody ProductAttributeEditDto editDto) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.editAttribute(editDto, id, attribute)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/pricing", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPricing(@PathVariable String id, @RequestBody ProductPricingCreateDto productPricingCreateDto) {
        try {
            productService.addPricing(productPricingCreateDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

//    @RequestMapping(value = "{id}/pricing", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPricing(@PathVariable String id, @RequestParam String pricing) {
        try {
            ProductPricingQueryDto productPricingQueryDto = new ProductPricingQueryDto();
            productPricingQueryDto.setProduct(id);
            productPricingQueryDto.setPricing(pricing);
            if (pricing.isEmpty()) {
                return ResponseEntity.ok(gson.toJson(productService.getPricings(id)));
            } else {
                return ResponseEntity.ok(gson.toJson(productService.getPricing(productPricingQueryDto)));
            }
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/pricing", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPricings(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.getPricings(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/pricing", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deletePricing(@PathVariable String id, @RequestParam String pricing) {
        try {
            ProductPricingPKEntity productPricingPKEntity = new ProductPricingPKEntity();
            productPricingPKEntity.setProduct(id);
            productPricingPKEntity.setPricing(pricing);
            productService.deletePricing(productPricingPKEntity);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/{id}/pricing", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPricing(@PathVariable String id,
                                         @RequestBody ProductPricingEditDto productPricingEditDto) {
        try {
            productService.editPricing(productPricingEditDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/category", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addCategory(@PathVariable String id, @RequestParam String category) {
        try {
            ProductCategoryProcessDto productCategoryProcessDto = new ProductCategoryProcessDto();
            productCategoryProcessDto.setProduct(id);
            productCategoryProcessDto.setCategory(category);
            productService.addCategory(productCategoryProcessDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/category", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCategory(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(productService.getCategories(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/category", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteCategory(@PathVariable String id, @RequestParam String category) {
        try {
            ProductCategoryProcessDto productCategoryProcessDto = new ProductCategoryProcessDto();
            productCategoryProcessDto.setProduct(id);
            productCategoryProcessDto.setCategory(category);
            productService.deleteCategory(productCategoryProcessDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

}
