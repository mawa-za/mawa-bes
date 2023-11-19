package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeCreateDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeEditDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingCreateDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingEditDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.entity.ProductAttributePKEntity;
import za.co.mawa.bes.entity.ProductPricingPKEntity;
import za.co.mawa.bes.exception.*;

import java.util.ArrayList;
import java.util.List;

public interface ProductDao {
    ProductDto create(ProductCreateDto productCreateDto) throws ProductCreationFailure;

    List<ProductDto> search(ProductQueryDto productQueryDto);

    ProductDto get(String id) throws ProductNotFoundException;

    void edit(ProductDto productDto) throws ProductUpdateFailure;

    void delete(String id) throws ProductDeleteFailure;

    void addPricing(ProductPricingCreateDto productPricingCreateDto) throws Exception;

    void editPricing(ProductPricingEditDto productPricingEditDto) throws Exception;

    ProductPricingDto getPricing(ProductPricingQueryDto productPricingQueryDto) throws DoesNotExist;
    List<ProductPricingDto> getPricings(String product);

    ProductDto getOptionalById(String id);

    void deletePricing(ProductPricingPKEntity productPricingPK) throws ProductDeleteFailure;

    ProductAttributeDto getAttribute(ProductAttributeQueryDto productAttributeQueryDto);

    ArrayList<ProductAttributeDto> getAttributes(ProductAttributeQueryDto queryDto);

    ArrayList<ProductAttributeDto> getAttributes(String id);

    boolean addAttribute(ProductAttributeCreateDto createDto) throws Exception;

    boolean editAttribute(ProductAttributeEditDto editDto, String product, String attribute) throws Exception;

    boolean deleteAttribute(ProductAttributePKEntity pkEntity) throws Exception;

}
