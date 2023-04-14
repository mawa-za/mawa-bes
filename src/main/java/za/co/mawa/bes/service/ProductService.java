package za.co.mawa.bes.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ProductDao;
import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.entity.ProductPricingEntity;
import za.co.mawa.bes.entity.ProductPricingPKEntity;
import za.co.mawa.bes.exception.ProductCreationFailure;
import za.co.mawa.bes.exception.ProductDeleteFailure;
import za.co.mawa.bes.exception.ProductNotFound;
import za.co.mawa.bes.exception.ProductUpdateFailure;
import za.co.mawa.bes.repository.ProductPricingRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.NumberRangeType;
import za.co.mawa.bes.utils.PriceType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService implements ProductDao {
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ProductPricingRepository productPricingRepository;

    @Autowired
    NumberRangeService numberRangeService;

    @Override
    public ProductDto create(ProductCreateDto productCreateDto) throws ProductCreationFailure {
        try {
            ProductEntity productEntity = new ProductEntity();
            if(productCreateDto.getCode() != null && productCreateDto.getCode() != "")
            {
                productEntity.setCode(productCreateDto.getCode());
            }
            else {
                String autogenerate = productCreateDto.getAutoGenerateCode() == null ? "" : productCreateDto.getAutoGenerateCode();
                if(autogenerate.toUpperCase().equalsIgnoreCase("X"))
                {
                    productEntity.setCode(numberRangeService.generateNumber(NumberRangeType.PRODUCT));
                }
            }
            productEntity.setDescription(productCreateDto.getDescription());
            productEntity.setCategory(productCreateDto.getCategory().toUpperCase());
            productEntity.setValidFrom(new Date());
            productEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            productEntity.setUom(productCreateDto.getBaseUnitOfMeasure().toUpperCase());
            ProductDto productDto = new ProductDto(productRepository.save(productEntity));

            ProductPricingDto productPricingDto = new ProductPricingDto();
            productPricingDto.setProduct(productEntity.getId());
            if(productCreateDto.getPricingType() != null && productCreateDto.getPricingType() != "")
            {
                productPricingDto.setPricing(productCreateDto.getPricingType());
            }
            else {
                productPricingDto.setPricing(PriceType.SELLING_PRICE);
            }
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
            String code = productEntity.getCode() == null ? "":productEntity.getCode();
            productDto.setCode(code);
            productDto.setDescription(productEntity.getDescription());
            productDto.setCategory(productEntity.getCategory());
            productDtoList.add(productDto);
        }
        return productDtoList;
    }

    @Override
    public ProductDto get(String id) throws ProductNotFound {
        try {
            ProductEntity productEntity = productRepository.getById(id);
            ProductDto productDto = new ProductDto();
            productDto.setId(productEntity.getId());
            String code = productEntity.getCode() == null ? "":productEntity.getCode();
            productDto.setCode(code);
            productDto.setDescription(productEntity.getDescription());
            productDto.setCategory(productEntity.getCategory());
            productDto.setBaseUnitOfMeasure(productEntity.getUom());

            ProductPricingPKEntity pk = new ProductPricingPKEntity();
            pk.setProduct(id);
            pk.setPricing(PriceType.SELLING_PRICE);
            ProductPricingEntity entity = productPricingRepository.getById(pk);
            if(entity != null)
            {
                productDto.setSellingPrice(entity.getValue());
            }
            return productDto;
        } catch (EntityNotFoundException exception) {
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
            //productPricingRepository.deleteById();
        } catch (Exception exception) {
            throw new ProductDeleteFailure();
        }
    }

    @Override
    public void addPricing(ProductPricingDto productPricingDto) throws Exception{
        try{
            ProductPricingPKEntity pkEntity = new ProductPricingPKEntity();
            ProductPricingEntity entity = new ProductPricingEntity();
            pkEntity.setProduct(productPricingDto.getProduct());
            pkEntity.setPricing(productPricingDto.getPricing());
            entity.setValue(productPricingDto.getValue());
            entity.setProductPricingPKEntity(pkEntity);
            productPricingRepository.save(entity);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void editPricing(ProductPricingDto productPricingDto) throws Exception {

    }

    @Override
    public ProductDto getOptionalById(String id) {
//        ProductEntity productEntity = productRepository.findById(id);
        Optional<ProductEntity> productEntity = productRepository.findById(id);
        ProductDto productDto = new ProductDto();
        ProductEntity product = productEntity.orElse(null);
        if (product != null) {

            productDto.setId(productEntity.get().getId());
            productDto.setCode(productEntity.get().getCode());
            productDto.setDescription(productEntity.get().getDescription());
            productDto.setCategory(productEntity.get().getCategory());
        }else {

//            Optional<ProductEntity> product = Optional.of(productEntity);
            return null;
        }

        return productDto;

    }
}
