package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ProductDao;
import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.exception.ProductCreationFailure;
import za.co.mawa.bes.exception.ProductDeleteFailure;
import za.co.mawa.bes.exception.ProductNotFound;
import za.co.mawa.bes.exception.ProductUpdateFailure;
import za.co.mawa.bes.repository.ProductPricingRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ProductService implements ProductDao {
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ProductPricingRepository productPricingRepository;

    @Override
    public ProductDto create(ProductCreateDto productCreateDto) throws ProductCreationFailure {
        try {
            ProductEntity productEntity = new ProductEntity();
            productEntity.setCode(productCreateDto.getCode());
            productEntity.setDescription(productCreateDto.getDescription());
            productEntity.setCategory(productCreateDto.getCategory());
            productEntity.setValidFrom(new Date());
            productEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            ProductDto productDto = new ProductDto(productRepository.save(productEntity));

            ProductPricingDto productPricingDto = new ProductPricingDto();
            productPricingDto.setProduct(productEntity.getId());
            productPricingDto.setPricing("SELLING-PRICE");
            productPricingDto.setValue(productCreateDto.getSellingPrice());

            addPricing(productPricingDto);

            return productDto;
        } catch (Exception exception) {
            throw new ProductCreationFailure();
        }
    }

    @Override
    public List<ProductDto> search(ProductQueryDto productQueryDto) {
        List<ProductDto> productDtoList = new ArrayList<>();
        List<ProductEntity> productEntityList = productRepository.findAll();
        for (ProductEntity productEntity : productEntityList) {
            ProductDto productDto = new ProductDto();
            productDto.setId(productEntity.getId());
            productDto.setCode(productEntity.getCode());
            productDto.setDescription(productEntity.getDescription());
            productDto.setCategory(productEntity.getCategory());
            productDtoList.add(productDto);
        }
        return productDtoList;
    }

    @Override
    public ProductDto get(String id) throws ProductNotFound {
        ProductEntity productEntity = productRepository.getById(id);
        if (productEntity != null) {
            ProductDto productDto = new ProductDto();
            productDto.setId(productEntity.getId());
            productDto.setCode(productEntity.getCode());
            productDto.setDescription(productEntity.getDescription());
            productDto.setCategory(productEntity.getCategory());
            return productDto;
        } else {
            throw new ProductNotFound();
        }

    }

    @Override
    public void edit(ProductDto productDto) throws ProductUpdateFailure {
        try {
            ProductEntity productEntity = new ProductEntity();
            productEntity.setId(productDto.getId());
            productEntity.setCode(productDto.getCode());
            productEntity.setDescription(productDto.getDescription());
            productEntity.setCategory(productDto.getCategory());
            productRepository.save(productEntity);
        } catch (Exception exception) {
            throw new ProductUpdateFailure();
        }
    }

    @Override
    public void delete(String id) throws ProductDeleteFailure {
        try {
        productRepository.deleteById(id);
        } catch (Exception exception) {
            throw new ProductDeleteFailure();
        }
    }

    @Override
    public void addPricing(ProductPricingDto productPricingDto) {

    }

    @Override
    public void editPricing(ProductPricingDto productPricingDto) {

    }
}
