package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeCreateDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeEditDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.entity.ProductAttributeEntity;
import za.co.mawa.bes.entity.ProductAttributePKEntity;
import za.co.mawa.bes.entity.ProductPricingPKEntity;
import za.co.mawa.bes.exception.ProductCreationFailure;
import za.co.mawa.bes.exception.ProductDeleteFailure;
import za.co.mawa.bes.exception.ProductNotFound;
import za.co.mawa.bes.exception.ProductUpdateFailure;

import java.util.ArrayList;
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

    void deletePricing(ProductPricingPKEntity productPricingPK) throws ProductDeleteFailure;
    ArrayList<ProductAttributeDto> getAttributes(ProductAttributeQueryDto queryDto);
    boolean addAttribute(ProductAttributeCreateDto createDto)throws Exception;
    boolean editAttribute(ProductAttributeEditDto editDto,String product,String attribute) throws Exception;
    boolean deleteAttribute(ProductAttributePKEntity pkEntity) throws Exception;

}
