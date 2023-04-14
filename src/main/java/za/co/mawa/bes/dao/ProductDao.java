package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.exception.ProductCreationFailure;
import za.co.mawa.bes.exception.ProductDeleteFailure;
import za.co.mawa.bes.exception.ProductNotFound;
import za.co.mawa.bes.exception.ProductUpdateFailure;

import java.util.List;

public interface ProductDao {
    ProductDto create(ProductCreateDto productCreateDto) throws ProductCreationFailure;
    List<ProductDto> search(ProductQueryDto productQueryDto);
    ProductDto get(String id) throws ProductNotFound;
    void edit(ProductDto productDto) throws ProductUpdateFailure;
    void delete(String id) throws ProductDeleteFailure;
    void addPricing(ProductPricingDto productPricingDto) throws Exception;
    void editPricing(ProductPricingDto productPricingDto) throws Exception;

    ProductDto getOptionalById(String id);

}
