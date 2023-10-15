package za.co.mawa.bes.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ProductDao;
import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.ProductQueryDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeCreateDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeEditDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingEditDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.exception.ProductCreationFailure;
import za.co.mawa.bes.exception.ProductDeleteFailure;
import za.co.mawa.bes.exception.ProductNotFoundException;
import za.co.mawa.bes.exception.ProductUpdateFailure;
import za.co.mawa.bes.repository.ProductAttributeRepository;
import za.co.mawa.bes.repository.ProductPricingRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.utils.*;

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
    ProductAttributeRepository productAttributeRepository;

    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    FieldOptionService fieldOptionService;

    @Override
    public ProductDto create(ProductCreateDto productCreateDto) throws ProductCreationFailure {
        try {
            ProductEntity productEntity = new ProductEntity();
            if (productCreateDto.getCode() != null && productCreateDto.getCode() != "") {
                productEntity.setCode(productCreateDto.getCode());
            } else {
                String autogenerate = productCreateDto.getAutoGenerateCode() == null ? "" : productCreateDto.getAutoGenerateCode();
                if (autogenerate.toUpperCase().equalsIgnoreCase("X")) {
                    productEntity.setCode(numberRangeService.generateNumber(NumberRangeType.PRODUCT));
                }
            }
            productEntity.setDescription(productCreateDto.getDescription());
            productEntity.setCategory(productCreateDto.getCategory().toUpperCase());
            productEntity.setValidFrom(new Date());
            productEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            productEntity.setUom(productCreateDto.getBaseUnitOfMeasure().toUpperCase());
            ProductDto productDto = new ProductDto(productRepository.save(productEntity));

            if (productCreateDto.getCategory().toUpperCase().equals(ProductCategory.MEMBERSHIP)) {
                ProductAttributeCreateDto productAttributeCreateDto = new ProductAttributeCreateDto();
                productAttributeCreateDto.setProduct(productEntity.getId());
                productAttributeCreateDto.setAttribute(ProductAttribute.WAITING_PERIOD);
                productAttributeCreateDto.setValue("0");
                addAttribute(productAttributeCreateDto);
            }

            ProductPricingDto productPricingDto = new ProductPricingDto();
            productPricingDto.setProduct(productEntity.getId());
            if (productCreateDto.getPricingType() != null && productCreateDto.getPricingType() != "") {
                productPricingDto.setPricing(productCreateDto.getPricingType());
            } else {
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
        List<ProductEntity> productEntityList = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        productEntityList = productRepository.findAll(findByCriteria(productQueryDto), sort);
        for (ProductEntity productEntity : productEntityList) {
            ProductDto productDto = new ProductDto();
            productDto.setId(productEntity.getId());
            String code = productEntity.getCode() == null ? "" : productEntity.getCode();
            productDto.setCode(code);
            productDto.setDescription(productEntity.getDescription());
            productDto.setCategory(productEntity.getCategory());
            productDto.setBaseUnitOfMeasure(productEntity.getUom());
            productDto.setCategoryDescription(fieldOptionService.getFieldOptionDescription("PRODUCT-CATEGORY", productEntity.getCategory()));
            productDto.setBaseUnitOfMeasureDescription(fieldOptionService.getFieldOptionDescription("UOM", productEntity.getUom()));
            for (ProductPricingEntity price : productPricingRepository.findPricing(productEntity.getId())) {
                // if(price.getProductPricingPKEntity().getPricing().equalsIgnoreCase(PriceType.SELLING_PRICE)){
                productDto.setSellingPrice(price.getValue());
                productDto.setPriceTypeDescription(fieldOptionService.getFieldOptionDescription("PRICING_TYPE", price.getProductPricingPKEntity().getPricing()));
                //     break;
                // }
            }
            productDtoList.add(productDto);
        }
        return productDtoList;
    }

    @Override
    public ProductDto get(String id) throws ProductNotFoundException {
        try {
            ProductEntity productEntity = productRepository.getById(id);
            ProductDto productDto = new ProductDto();
            productDto.setId(productEntity.getId());
            String code = productEntity.getCode() == null ? "" : productEntity.getCode();
            productDto.setCode(code);
            productDto.setDescription(productEntity.getDescription());
            productDto.setCategory(productEntity.getCategory());
            productDto.setCategoryDescription(fieldOptionService.getFieldOptionDescription("PRODUCT-CATEGORY", productEntity.getCategory()));
            productDto.setBaseUnitOfMeasure(productEntity.getUom());
            productDto.setBaseUnitOfMeasureDescription(fieldOptionService.getFieldOptionDescription("UOM", productEntity.getUom()));
            productDto.setValidTo(Conversion.dateToString(productEntity.getValidTo()));
            productDto.setValidFrom(Conversion.dateToString(productEntity.getValidFrom()));
            for (ProductPricingEntity price : productPricingRepository.findPricing(id)) {
                //  if(price.getProductPricingPKEntity().getPricing().equalsIgnoreCase(PriceType.SELLING_PRICE)){
                productDto.setSellingPrice(price.getValue());
                productDto.setPriceType(price.getProductPricingPKEntity().getPricing());
                productDto.setPriceTypeDescription(fieldOptionService.getFieldOptionDescription("PRICING_TYPE", price.getProductPricingPKEntity().getPricing()));
                //     break;
                // }
            }
            return productDto;
        } catch (EntityNotFoundException exception) {
            throw new ProductNotFoundException();
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
            productEntity.setUom(productDto.getBaseUnitOfMeasure().toUpperCase());
            productRepository.save(productEntity);
        } catch (Exception exception) {
            throw new ProductUpdateFailure();
        }
    }

    @Override
    public void delete(String id) throws ProductDeleteFailure {
        try {
            productRepository.deleteById(id);
            for (ProductPricingEntity price : productPricingRepository.findPricing(id)) {
                deletePricing(price.getProductPricingPKEntity());
            }
        } catch (Exception exception) {
            throw new ProductDeleteFailure();
        }
    }

    @Override
    public void addPricing(ProductPricingDto productPricingDto) throws Exception {
        try {
            ProductPricingPKEntity pkEntity = new ProductPricingPKEntity();
            ProductPricingEntity entity = new ProductPricingEntity();
            pkEntity.setProduct(productPricingDto.getProduct());
            pkEntity.setPricing(productPricingDto.getPricing());
            entity.setValue(productPricingDto.getValue());
            entity.setProductPricingPKEntity(pkEntity);
            productPricingRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void editPricing(ProductPricingEditDto productPricingEditDto) throws Exception {
        try {
            ProductPricingPKEntity pkEntity = new ProductPricingPKEntity();
            ProductPricingEntity entity = new ProductPricingEntity();
            pkEntity.setProduct(productPricingEditDto.getProduct());
            pkEntity.setPricing(productPricingEditDto.getPricing());
            entity.setValue(productPricingEditDto.getValue());
            entity.setProductPricingPKEntity(pkEntity);
            productPricingRepository.save(entity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public ProductPricingDto getPricing(ProductPricingQueryDto productPricingQueryDto) {
        return null;
    }

    @Override
    public List<ProductPricingDto> getPricings(String product) {
        return null;
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
        } else {

//            Optional<ProductEntity> product = Optional.of(productEntity);
            return null;
        }

        return productDto;

    }

    @Override
    public void deletePricing(ProductPricingPKEntity productPricingPK) throws ProductDeleteFailure {
        try {
            productPricingRepository.deleteById(productPricingPK);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ProductAttributeDto getAttribute(ProductAttributeQueryDto productAttributeQueryDto) {
        try {
            ProductAttributePKEntity productAttributePKEntity = new ProductAttributePKEntity();
            productAttributePKEntity.setProduct(productAttributeQueryDto.getProduct());
            productAttributePKEntity.setAttribute(productAttributeQueryDto.getAttribute());
            ProductAttributeEntity productAttributeEntity = productAttributeRepository.getById(productAttributePKEntity);
            ProductAttributeDto productAttributeDto = new ProductAttributeDto();
            productAttributeDto.setAttribute(productAttributeEntity.getProductAttributePKEntity().getAttribute());
            productAttributeDto.setProduct(productAttributeEntity.getProductAttributePKEntity().getProduct());
            productAttributeDto.setValue(productAttributeEntity.getValue());
            productAttributeDto.setValidFrom(Conversion.dateToString(productAttributeEntity.getValidFrom()));
            productAttributeDto.setValidTo(Conversion.dateToString(productAttributeEntity.getValidTo()));
            return productAttributeDto;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public ArrayList<ProductAttributeDto> getAttributes(ProductAttributeQueryDto queryDto) {
        try {
            ArrayList<ProductAttributeDto> attributes = new ArrayList<>();
            Sort sort = Sort.by("productAttributePKEntity").descending();
            for (ProductAttributeEntity attributeEntity : productAttributeRepository.findAll(findByAttribute(queryDto), sort)) {
                ProductAttributeDto productAttributeDto = new ProductAttributeDto();
                productAttributeDto.setAttribute(attributeEntity.getProductAttributePKEntity().getAttribute());
                productAttributeDto.setProduct(attributeEntity.getProductAttributePKEntity().getProduct());
                productAttributeDto.setValue(attributeEntity.getValue());
                productAttributeDto.setValidFrom(Conversion.dateToString(attributeEntity.getValidFrom()));
                productAttributeDto.setValidTo(Conversion.dateToString(attributeEntity.getValidTo()));
                attributes.add(productAttributeDto);
            }
            return attributes;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean addAttribute(ProductAttributeCreateDto createDto) throws Exception {
        try {
            ProductAttributePKEntity pkEntity = new ProductAttributePKEntity();
            ProductAttributeEntity entity = new ProductAttributeEntity();
            pkEntity.setAttribute(createDto.getAttribute());
            pkEntity.setProduct(createDto.getProduct());
            entity.setValue(createDto.getValue());
            entity.setValidFrom(new Date());
            entity.setValidTo(Conversion.stringToDate("9999-12-31"));
            entity.setProductAttributePKEntity(pkEntity);
            productAttributeRepository.save(entity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean editAttribute(ProductAttributeEditDto editDto, String product, String attribute) throws Exception {
        try {
            ProductAttributePKEntity entityPk = new ProductAttributePKEntity();
            entityPk.setProduct(product);
            entityPk.setAttribute(attribute);
            ProductAttributeEntity entity = productAttributeRepository.getById(entityPk);
            if (editDto.getValue() != null) {
                entity.setValue(editDto.getValue());
            }
            if (editDto.getValidFrom() != null) {
                entity.setValidFrom(Conversion.stringToDate(editDto.getValidFrom()));
            }
            if (editDto.getValidTo() != null) {
                entity.setValidTo(Conversion.stringToDate(editDto.getValidTo()));
            }
            productAttributeRepository.save(entity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public boolean deleteAttribute(ProductAttributePKEntity pkEntity) throws Exception {
        try {
            productAttributeRepository.deleteById(pkEntity);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Specification<ProductEntity> findByCriteria(ProductQueryDto productQuery) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (productQuery.getCode() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("code"), productQuery.getCode()));
            }
            if (productQuery.getCategory() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category"), productQuery.getCategory()));
            }
            return predicate;
        };
    }

    private Specification<ProductAttributeEntity> findByAttribute(ProductAttributeQueryDto attributeQuery) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (attributeQuery.getAttribute() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("productAttributePKEntity").get("attribute"), attributeQuery.getAttribute()));
            }
            if (attributeQuery.getProduct() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("productAttributePKEntity").get("product"), attributeQuery.getProduct()));
            }
            if (attributeQuery.getValue() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("value"), attributeQuery.getValue()));
            }
            if (attributeQuery.getValidTo() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("validTo"), attributeQuery.getValidTo()));
            }
            if (attributeQuery.getValidFrom() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("validFrom"), attributeQuery.getValidFrom()));
            }
            return predicate;
        };
    }
}
