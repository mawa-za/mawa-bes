package za.co.mawa.bes.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ProductDao;
import za.co.mawa.bes.dto.product.ProductBasicDto;
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
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.exception.*;
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

            ProductPricingCreateDto productPricingCreateDto = new ProductPricingCreateDto();
            productPricingCreateDto.setProduct(productEntity.getId());
            if (productCreateDto.getPricingType() != null && productCreateDto.getPricingType() != "") {
                productPricingCreateDto.setPricing(productCreateDto.getPricingType());
            } else {
                productPricingCreateDto.setPricing(PriceType.SELLING_PRICE);
            }
            productPricingCreateDto.setValue(productCreateDto.getPrice());
            productPricingCreateDto.setValidFrom(new Date());
            productDto.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            addPricing(productPricingCreateDto);
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
            productDto.setCategoryDescription(fieldOptionService.getFieldOptionDescription(Field.PRODUCT_CATEGORY, productEntity.getCategory()));
            productDto.setBaseUnitOfMeasureDescription(fieldOptionService.getFieldOptionDescription(Field.UOM, productEntity.getUom()));
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
            productDto.setCategoryDescription(fieldOptionService.getFieldOptionDescription(Field.PRODUCT_CATEGORY, productEntity.getCategory()));
            productDto.setBaseUnitOfMeasure(productEntity.getUom());
            productDto.setBaseUnitOfMeasureDescription(fieldOptionService.getFieldOptionDescription(Field.UOM, productEntity.getUom()));
            productDto.setValidTo(productEntity.getValidTo());
            productDto.setValidFrom(productEntity.getValidFrom());
            productDto.setPricings(getPricings(id));
            productDto.setAttributes(getAttributes(id));
            return productDto;
        } catch (EntityNotFoundException exception) {
            throw new ProductNotFoundException();
        }
    }
    public ProductBasicDto getBasic(String id) throws ProductNotFoundException {
        try {
            ProductEntity productEntity = productRepository.getById(id);
            ProductBasicDto productBasicDto = new ProductBasicDto();
            productBasicDto.setId(productEntity.getId());
            String code = productEntity.getCode() == null ? "" : productEntity.getCode();
            productBasicDto.setCode(code);
            productBasicDto.setDescription(productEntity.getDescription());
            productBasicDto.setCategory(fieldOptionService.getFieldOption(Field.PRODUCT_CATEGORY, productEntity.getCategory()));
            productBasicDto.setBaseUnitOfMeasure(fieldOptionService.getFieldOption(Field.UOM, productEntity.getUom()));
            productBasicDto.setValidTo(productEntity.getValidTo());
            productBasicDto.setValidFrom(productEntity.getValidFrom());
            return productBasicDto;
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
            for (ProductPricingEntity price : productPricingRepository.findByProduct(id)) {
                deletePricing(price.getProductPricingPKEntity());
            }
        } catch (Exception exception) {
            throw new ProductDeleteFailure();
        }
    }

    @Override
    public void addPricing(ProductPricingCreateDto productPricingCreateDto) throws Exception {
        try {
            ProductPricingPKEntity pkEntity = new ProductPricingPKEntity();
            ProductPricingEntity entity = new ProductPricingEntity();
            pkEntity.setProduct(productPricingCreateDto.getProduct());
            pkEntity.setPricing(productPricingCreateDto.getPricing());
            entity.setValue(productPricingCreateDto.getValue());
            entity.setProductPricingPKEntity(pkEntity);
            entity.setValidFrom(productPricingCreateDto.getValidFrom());
            entity.setValidTo(productPricingCreateDto.getValidTo());
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
            entity.setValidFrom(productPricingEditDto.getValidFrom());
            entity.setValidTo(productPricingEditDto.getValidTo());
            productPricingRepository.save(entity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public ProductPricingDto getPricing(ProductPricingQueryDto productPricingQueryDto) throws DoesNotExist {
        try {
            ProductPricingPKEntity productPricingPKEntity = new ProductPricingPKEntity();
            productPricingPKEntity.setProduct(productPricingQueryDto.getProduct());
            productPricingPKEntity.setPricing(productPricingQueryDto.getPricing());
            ProductPricingEntity productPricingEntity = productPricingRepository.getById(productPricingPKEntity);
            ProductPricingDto productPricingDto = new ProductPricingDto();
            productPricingDto.setPricing(productPricingEntity.getProductPricingPKEntity().getPricing());
            productPricingDto.setPricingDescription(fieldOptionService.getFieldOptionDescription(Field.PRODUCT_PRICING, productPricingEntity.getProductPricingPKEntity().getPricing()));
            productPricingDto.setValue(productPricingEntity.getValue());
            productPricingDto.setValidFrom(productPricingEntity.getValidFrom());
            productPricingDto.setValidTo(productPricingEntity.getValidTo());
            return productPricingDto;
        } catch (Exception exception) {
            throw new DoesNotExist();
        }
    }

    @Override
    public List<ProductPricingDto> getPricings(String product) {
        List<ProductPricingDto> productPricingDtoList = new ArrayList<>();
        try {
            List<ProductPricingEntity> productPricingEntityList = productPricingRepository.findByProduct(product);
            for(ProductPricingEntity productPricingEntity: productPricingEntityList) {
                ProductPricingDto productPricingDto = new ProductPricingDto();
                productPricingDto.setPricing(productPricingEntity.getProductPricingPKEntity().getPricing());
                productPricingDto.setPricingDescription(fieldOptionService.getFieldOptionDescription(Field.PRODUCT_PRICING, productPricingEntity.getProductPricingPKEntity().getPricing()));
                productPricingDto.setValue(productPricingEntity.getValue());
                productPricingDto.setValidFrom(productPricingEntity.getValidFrom());
                productPricingDto.setValidTo(productPricingEntity.getValidTo());
                productPricingDtoList.add(productPricingDto);
            }
        } catch (Exception exception) {

        }
        return productPricingDtoList;
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
            productAttributeDto.setAttributeDescription(fieldOptionService.getFieldOptionDescription(Field.PRODUCT_ATTRIBUTE, productAttributeEntity.getProductAttributePKEntity().getAttribute()));
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
        return null;
    }

    @Override
    public ArrayList<ProductAttributeDto> getAttributes(String id) {
        try {
            ArrayList<ProductAttributeDto> attributes = new ArrayList<>();
            for (ProductAttributeEntity attributeEntity : productAttributeRepository.findByProduct(id)) {
                ProductAttributeDto productAttributeDto = new ProductAttributeDto();
                productAttributeDto.setAttribute(attributeEntity.getProductAttributePKEntity().getAttribute());
                productAttributeDto.setAttributeDescription(fieldOptionService.getFieldOptionDescription(Field.PRODUCT_ATTRIBUTE, attributeEntity.getProductAttributePKEntity().getAttribute()));
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
