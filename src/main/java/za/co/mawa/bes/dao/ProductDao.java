package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.ProductCreateDto;
import za.co.mawa.bes.dto.ProductDto;
import za.co.mawa.bes.dto.ProductPricingDto;
import za.co.mawa.bes.dto.ProductQueryDto;
import za.co.mawa.bes.exception.ProductCreationFailure;

import java.util.List;

public interface ProductDao {
    ProductDto create(ProductCreateDto productCreateDto) throws ProductCreationFailure;
    List<ProductDto> search(ProductQueryDto productQueryDto);
    ProductDto get(String id);
    void edit(ProductDto productDto);
    void delete(String id);
    void addPricing(ProductPricingDto productPricingDto);

}
