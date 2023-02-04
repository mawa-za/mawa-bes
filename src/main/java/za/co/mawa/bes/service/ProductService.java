package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ProductDao;
import za.co.mawa.bes.dto.ProductCreateDto;
import za.co.mawa.bes.dto.ProductDto;
import za.co.mawa.bes.dto.ProductPricingDto;
import za.co.mawa.bes.dto.ProductQueryDto;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.exception.ProductCreationFailure;
import za.co.mawa.bes.repository.ProductPricingRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.utils.Constant;

import java.util.Date;

@Service
public class ProductService implements ProductDao {
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ProductPricingRepository productPricingRepository;
    @Override
    public ProductDto create(ProductCreateDto productCreateDto) throws ProductCreationFailure {
        try{
        ProductEntity productEntity = new ProductEntity();
        productEntity.setCode(productCreateDto.getCode());
        productEntity.setDescription(productCreateDto.getDescription());
        productEntity.setCategory(productCreateDto.getCategory());
        productEntity.setValidFrom(new Date());
        productEntity.setValidFrom(new Date(Constant.END_DATE));
        ProductDto productDto = new ProductDto(productRepository.save(productEntity));

        ProductPricingDto productPricingDto = new ProductPricingDto();
        productPricingDto.setProduct(productEntity.getId());
        productPricingDto.setPricing("SELLING-PRICE");
        productPricingDto.setValue(productCreateDto.getSellingPrice());

        addPricing(productPricingDto);

        return productDto;}
        catch (Exception exception){
            throw new ProductCreationFailure();
        }
    }

    @Override
    public ProductDto search(ProductQueryDto productQueryDto) {
        return null;
    }

    @Override
    public ProductDto get(String id) {
        return null;
    }

    @Override
    public void edit(ProductDto productDto) {

    }

    @Override
    public void delete(String id) {

    }

    @Override
    public void addPricing(ProductPricingDto productPricingDto) {

    }
}
