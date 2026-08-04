package za.co.mawa.bes.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.ProductDao;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.product.*;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeCreateDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeEditDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryCreateDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryProcessDto;
import za.co.mawa.bes.dto.product.classification.ProductCategoryDefinitionDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingCreateDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingEditDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.entity.product.ProductCategoryEntity;
import za.co.mawa.bes.entity.product.ProductCategoryMasterEntity;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.enums.ProductTypeCode;
import za.co.mawa.bes.repository.ProductAttributeRepository;
import za.co.mawa.bes.repository.ProductCategoryRepository;
import za.co.mawa.bes.repository.ProductPricingRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.service.v2.ProductClassificationService;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ProductService implements ProductDao {
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ProductPricingRepository productPricingRepository;
    @Autowired
    ProductAttributeRepository productAttributeRepository;
    @Autowired
    ProductCategoryRepository productCategoryRepository;
    @Autowired
    NumberRangeService numberRangeService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    ProductClassificationService productClassificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDto create(ProductCreateDto productCreateDto) throws ProductCreationFailure {
        try {
            if (productCreateDto.getType() == null || productCreateDto.getType().isBlank()) {
                throw new IllegalArgumentException("Product type is required.");
            }
            ProductTypeCode productType = ProductTypeCode.requireSelectable(productCreateDto.getType());
            if (ProductTypeCode.FUNERAL_PACKAGE == productType) {
                throw new IllegalArgumentException("Create funeral packages from Funeral Package Setup so that pricing and package composition remain linked.");
            }
            String requestedCategory = firstNonBlank(productCreateDto.getCategoryId(), productCreateDto.getCategory());
            ProductCategoryMasterEntity category = productClassificationService.requireActiveCategory(
                    requestedCategory, productType.getCode());

            ProductEntity productEntity = new ProductEntity();
            if (productCreateDto.getCode() != null && !productCreateDto.getCode().isBlank()) {
                String productCode = productCreateDto.getCode().trim().toUpperCase();
                if (productRepository.findByCode(productCode) != null) {
                    throw new IllegalArgumentException("A product with code " + productCode + " already exists.");
                }
                productEntity.setCode(productCode);
            } else {
                String autogenerate = productCreateDto.getAutoGenerateCode() == null ? "" : productCreateDto.getAutoGenerateCode();
                if (autogenerate.equalsIgnoreCase("X")) {
                    productEntity.setCode(numberRangeService.generateNumber(NumberRangeType.PRODUCT));
                }
            }
            productEntity.setDescription(productCreateDto.getDescription() == null ? "" : productCreateDto.getDescription().trim().toUpperCase());
            productEntity.setType(productType.getCode());
            productEntity.setCategoryId(category.getId());
            productEntity.setAvailableForSale(productCreateDto.getAvailableForSale() == null
                    ? productType.isDefaultAvailableForSale()
                    : productCreateDto.getAvailableForSale());
            productEntity.setValidFrom(new Date());
            productEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            productEntity.setUom(productCreateDto.getBaseUnitOfMeasure() == null ? "EA" : productCreateDto.getBaseUnitOfMeasure().trim().toUpperCase());
            ProductDto productDto = get(productRepository.save(productEntity).getId());
            writeProductAudit(productDto.getId(), "CREATE", null,
                    productDto.getCode() + " - " + productDto.getDescription() + " [" + productType.getCode() + "]", null);
            if (productCreateDto.getPricingType() != null && !productCreateDto.getPricingType().isBlank()) {
                ProductPricingCreateDto productPricingCreateDto = new ProductPricingCreateDto();
                productPricingCreateDto.setProduct(productDto.getId());
                productPricingCreateDto.setPricing(productCreateDto.getPricingType().trim().toUpperCase());
                productPricingCreateDto.setValue(productCreateDto.getPrice() == null ? BigDecimal.ZERO : productCreateDto.getPrice());
                productPricingCreateDto.setValidFrom(new Date());
                productPricingCreateDto.setValidTo(Conversion.stringToDate(Constant.END_DATE));
                addPricing(productPricingCreateDto);
            }
            return productDto;
        } catch (Exception exception) {
            throw new ProductCreationFailure(exception.getMessage());
        }
    }

    @Override
    public List<ProductDto> search(ProductQueryDto productQueryDto) {
        List<ProductDto> productDtoList = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        List<ProductEntity> productEntityList = productRepository.findAll(findByCriteria(productQueryDto), sort);
        for (ProductEntity productEntity : productEntityList) {
            ProductDto product = entityToDto(productEntity);
            if (product != null) {
                productDtoList.add(product);
            }
        }
        return productDtoList;
    }

    @Override
    public ProductDto get(String id) throws ProductNotFoundException {
        try {
            ProductEntity productEntity = productRepository.getById(id);
            ProductDto productDto = entityToDto(productEntity);
            if (productDto == null) {
                throw new ProductNotFoundException();
            }
            return productDto;
        } catch (EntityNotFoundException exception) {
            throw new ProductNotFoundException();
        }
    }

    public ProductDto getByCode(String code) throws ProductNotFoundException {
        try {
            ProductEntity productEntity = productRepository.findByCode(code);
            return get(productEntity.getId());
        } catch (EntityNotFoundException exception) {
            throw new ProductNotFoundException();
        }
    }

    public List<ProductDto> query(String type,String query)  {
        try {
            List<ProductDto> productDtoList = new ArrayList<>();
            String normalisedType = type == null || type.isBlank() ? null : ProductTypeCode.find(type).map(ProductTypeCode::getCode).orElse(type.trim().toUpperCase());
            List<ProductEntity> productEntities = productRepository.findByQuery(normalisedType, query);
            for(ProductEntity productEntity: productEntities){
                productDtoList.add(entityToDto(productEntity));
            }
            return productDtoList;
        } catch (EntityNotFoundException exception) {
            return new ArrayList<>();
        }

    }


    public ProductDto entityToDto(ProductEntity productEntity) {
        try {
            ProductDto productDto = new ProductDto();
            productDto.setId(productEntity.getId());
            productDto.setCode(productEntity.getCode() == null ? "" : productEntity.getCode());
            productDto.setDescription(productEntity.getDescription());
            productDto.setType(resolveProductTypeOption(productEntity.getType()));
            ProductTypeCode.find(productEntity.getType()).ifPresent(type -> productDto.setTypeBehaviour(type.toDto()));
            productDto.setAvailableForSale(Boolean.TRUE.equals(productEntity.getAvailableForSale()));
            if (productEntity.getCategoryId() != null && !productEntity.getCategoryId().isBlank()) {
                try {
                    productDto.setPrimaryCategory(productClassificationService.getCategory(productEntity.getCategoryId()));
                } catch (Exception ignored) {
                    // Keeps migrated products readable when a category was removed outside the application.
                }
            }
            productDto.setBaseUnitOfMeasure(fieldOptionService.getFieldOption(Field.UOM, productEntity.getUom()));
            productDto.setValidTo(productEntity.getValidTo());
            productDto.setValidFrom(productEntity.getValidFrom());
            productDto.setPricings(getPricings(productEntity.getId()));
            productDto.setAttributes(getAttributes(productEntity.getId()));
            productDto.setCategories(getCategories(productEntity.getId()));
            productDto.setBarcodes(getBarcodes(productEntity.getId()));
            try {
                List<Map<String, Object>> packageLinks = jdbcTemplate.queryForList(
                        "SELECT id FROM funeral_package WHERE product_id = ? LIMIT 1", productEntity.getId());
                productDto.setManagedByFuneralPackage(!packageLinks.isEmpty());
                if (!packageLinks.isEmpty()) {
                    productDto.setFuneralPackageId(Objects.toString(packageLinks.get(0).get("id"), null));
                }
            } catch (Exception ignored) {
                productDto.setManagedByFuneralPackage(false);
            }
            return productDto;
        } catch (EntityNotFoundException exception) {
            return null;
        }
    }

    public List<String> getBarcodes(String productId) {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT barcode FROM product_barcode WHERE product_id = ? ORDER BY is_primary DESC, created_at ASC",
                    String.class,
                    productId
            );
        } catch (Exception ignored) {
            // Keeps product reads backwards compatible while a tenant is still awaiting migration.
            return new ArrayList<>();
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
            productBasicDto.setType(resolveProductTypeOption(productEntity.getType()));
//            productBasicDto.setCategory(fieldOptionService.getFieldOption(Field.PRODUCT_CATEGORY, productEntity.getCategory()));
            productBasicDto.setBaseUnitOfMeasure(fieldOptionService.getFieldOption(Field.UOM, productEntity.getUom()));
            productBasicDto.setValidTo(productEntity.getValidTo());
            productBasicDto.setValidFrom(productEntity.getValidFrom());
            return productBasicDto;
        } catch (EntityNotFoundException exception) {
            throw new ProductNotFoundException();
        }
    }

    @Override
    public void edit(ProductEditDto productEditDto) throws ProductUpdateFailure {
        try {
            ProductEntity productEntity = productRepository.getById(productEditDto.getId());
            if (isManagedFuneralPackageProduct(productEntity.getId())
                    || ProductTypeCode.FUNERAL_PACKAGE == ProductTypeCode.find(productEntity.getType()).orElse(null)
                    || ProductTypeCode.FUNERAL_PACKAGE == ProductTypeCode.find(productEditDto.getType()).orElse(null)) {
                throw new IllegalArgumentException("Manage funeral package products from Funeral Package Setup.");
            }
            if (productEditDto.getCode() != null && !productEditDto.getCode().isBlank()) {
                productEntity.setCode(productEditDto.getCode().trim().toUpperCase());
            }
            if (productEditDto.getDescription() != null && !productEditDto.getDescription().isBlank()) {
                productEntity.setDescription(productEditDto.getDescription().trim().toUpperCase());
            }

            ProductTypeCode productType;
            if (productEditDto.getType() != null && !productEditDto.getType().isBlank()) {
                productType = ProductTypeCode.requireSelectable(productEditDto.getType());
                productEntity.setType(productType.getCode());
            } else {
                productType = ProductTypeCode.find(productEntity.getType())
                        .orElseThrow(() -> new IllegalArgumentException("Select one of the supported product types before saving this legacy product."));
            }

            String requestedCategory = firstNonBlank(productEditDto.getCategoryId(), productEditDto.getCategory());
            if (requestedCategory != null) {
                ProductCategoryMasterEntity category = productClassificationService.requireActiveCategory(
                        requestedCategory, productType.getCode());
                productEntity.setCategoryId(category.getId());
            } else if (productEntity.getCategoryId() == null || productEntity.getCategoryId().isBlank()) {
                throw new IllegalArgumentException("Product category is required.");
            } else {
                ProductCategoryMasterEntity category = productClassificationService.requireActiveCategory(
                        productEntity.getCategoryId(), productType.getCode());
                productEntity.setCategoryId(category.getId());
            }

            if (productEditDto.getAvailableForSale() != null) {
                productEntity.setAvailableForSale(productEditDto.getAvailableForSale());
            } else if (productEntity.getAvailableForSale() == null) {
                productEntity.setAvailableForSale(productType.isDefaultAvailableForSale());
            }
            if (productEditDto.getBaseUnitOfMeasure() != null && !productEditDto.getBaseUnitOfMeasure().isBlank()) {
                productEntity.setUom(productEditDto.getBaseUnitOfMeasure().trim().toUpperCase());
            }
            productRepository.save(productEntity);
            if (ProductTypeCode.SERVICE != productType) {
                try {
                    jdbcTemplate.update("UPDATE product_asset_link SET active = 0, updated_at = CURRENT_TIMESTAMP WHERE service_product_id = ?",
                            productEntity.getId());
                } catch (Exception ignored) {
                    // Keeps product maintenance compatible until the hire-asset migration has run.
                }
            }
            writeProductAudit(productEntity.getId(), "UPDATE", null,
                    productEntity.getCode() + " - " + productEntity.getDescription() + " [" + productEntity.getType() + "]", null);
            if (productEditDto.getPrice() != null) {
                ProductPricingCreateDto pricingCreateDto = new ProductPricingCreateDto();
                pricingCreateDto.setProduct(productEditDto.getId());
                pricingCreateDto.setPricing(productEditDto.getPricingType() == null || productEditDto.getPricingType().isBlank()
                        ? PriceType.SELLING_PRICE
                        : productEditDto.getPricingType().trim().toUpperCase());
                pricingCreateDto.setValue(productEditDto.getPrice());
                pricingCreateDto.setValidFrom(new Date());
                pricingCreateDto.setValidTo(Conversion.stringToDate(Constant.END_DATE));
                addPricing(pricingCreateDto);
            }
        } catch (Exception exception) {
            throw new ProductUpdateFailure(exception.getMessage());
        }
    }

    @Override
    public void delete(String id) throws ProductDeleteFailure {
        try {
            if (isManagedFuneralPackageProduct(id)) {
                throw new IllegalArgumentException("Deactivate the funeral package from Funeral Package Setup instead of deleting its linked product.");
            }
            for (ProductPricingEntity price : productPricingRepository.findByProduct(id)) {
                deletePricing(price.getProductPricingPKEntity());
            }
            writeProductAudit(id, "DELETE", id, null, null);
            jdbcTemplate.update("DELETE FROM product_barcode WHERE product_id = ?", id);
            productRepository.deleteById(id);
        } catch (Exception exception) {
            throw new ProductDeleteFailure(exception.getMessage());
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
            productPricingDto.setPricing(fieldOptionService.getFieldOption(Field.PRODUCT_PRICING, productPricingEntity.getProductPricingPKEntity().getPricing()));
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
            for (ProductPricingEntity productPricingEntity : productPricingEntityList) {
                ProductPricingDto productPricingDto = new ProductPricingDto();
                productPricingDto.setProduct(productPricingEntity.getProductPricingPKEntity().getProduct());
                productPricingDto.setPricing(fieldOptionService.getFieldOption(Field.PRICING_TYPE, productPricingEntity.getProductPricingPKEntity().getPricing()));
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
        try {
            ProductDto productDto = get(id);
            return productDto;
        } catch (Exception exception) {
            return null;
        }
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
            productAttributeDto.setAttribute(fieldOptionService.getFieldOption(Field.PRODUCT_ATTRIBUTE, productAttributeEntity.getProductAttributePKEntity().getAttribute()));
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
                productAttributeDto.setAttribute(fieldOptionService.getFieldOption(Field.PRODUCT_ATTRIBUTE, attributeEntity.getProductAttributePKEntity().getAttribute()));
                productAttributeDto.setProduct(attributeEntity.getProductAttributePKEntity().getProduct());
                productAttributeDto.setValue(attributeEntity.getValue());
                productAttributeDto.setValidFrom(Conversion.dateToString(attributeEntity.getValidFrom()));
                productAttributeDto.setValidTo(Conversion.dateToString(attributeEntity.getValidTo()));
                if (productAttributeDto.getAttribute() != null) {
                    attributes.add(productAttributeDto);
                }
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

    public void addCategory(ProductCategoryProcessDto productCategoryProcessDto) throws Exception {
        ProductEntity product = productRepository.getById(productCategoryProcessDto.getProduct());
        ProductTypeCode productType = ProductTypeCode.find(product.getType())
                .orElseThrow(() -> new IllegalArgumentException("Select a supported product type before assigning a category."));
        ProductCategoryMasterEntity category = productClassificationService.requireActiveCategory(
                productCategoryProcessDto.getCategory(), productType.getCode());
        product.setCategoryId(category.getId());
        productRepository.save(product);
    }

    public ArrayList<ProductCategoryDto> getCategories(String id) {
        ArrayList<ProductCategoryDto> categories = new ArrayList<>();
        try {
            ProductEntity product = productRepository.getById(id);
            if (product.getCategoryId() != null && !product.getCategoryId().isBlank()) {
                ProductCategoryDefinitionDto category = productClassificationService.getCategory(product.getCategoryId());
                FieldOptionDto option = new FieldOptionDto();
                option.setField(Field.PRODUCT_CATEGORY);
                option.setCode(category.getCode());
                option.setType("TENANT");
                option.setDescription(category.getFullPath());
                ProductCategoryDto dto = new ProductCategoryDto();
                dto.setProduct(id);
                dto.setCategory(option);
                dto.setValidFrom(product.getValidFrom());
                dto.setValidTo(product.getValidTo());
                categories.add(dto);
                return categories;
            }
        } catch (Exception ignored) {
            // Fall back to the legacy many-to-many category bridge below.
        }
        for (ProductCategoryEntity legacy : productCategoryRepository.findByProduct(id)) {
            ProductCategoryDto dto = new ProductCategoryDto();
            dto.setCategory(fieldOptionService.getFieldOption(Field.PRODUCT_CATEGORY, legacy.getCategory()));
            dto.setProduct(legacy.getProduct());
            dto.setValidFrom(legacy.getValidFrom());
            dto.setValidTo(legacy.getValidTo());
            categories.add(dto);
        }
        return categories;
    }

    public void deleteCategory(ProductCategoryProcessDto productCategoryProcessDto) throws Exception {
        ProductEntity product = productRepository.getById(productCategoryProcessDto.getProduct());
        if (product.getCategoryId() != null) {
            String selectedCategoryId = productClassificationService.resolveCategoryId(productCategoryProcessDto.getCategory());
            if (product.getCategoryId().equals(selectedCategoryId)) {
                throw new IllegalArgumentException("A product must have one primary category. Assign a replacement category instead of removing it.");
            }
        }
        for (ProductCategoryEntity legacy : productCategoryRepository.find(
                productCategoryProcessDto.getProduct(), productCategoryProcessDto.getCategory())) {
            productCategoryRepository.deleteById(legacy.getId());
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


    public void requireAvailableForSale(String productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product was not found: " + productId));
        if (!Boolean.TRUE.equals(product.getAvailableForSale())) {
            throw new IllegalArgumentException("Product " + product.getCode() + " is configured for internal use and cannot be added to a customer invoice.");
        }
    }

    private FieldOptionDto resolveProductTypeOption(String storedType) {
        return ProductTypeCode.find(storedType)
                .map(type -> {
                    FieldOptionDto option = new FieldOptionDto();
                    option.setField(Field.PRODUCT_TYPE);
                    option.setCode(type.getCode());
                    option.setType("SYSTEM");
                    option.setDescription(type.getDisplayName());
                    return option;
                })
                .orElseGet(() -> {
                    FieldOptionDto existing = fieldOptionService.getFieldOption(Field.PRODUCT_TYPE, storedType);
                    if (existing != null) {
                        return existing;
                    }
                    FieldOptionDto legacy = new FieldOptionDto();
                    legacy.setField(Field.PRODUCT_TYPE);
                    legacy.setCode(storedType == null ? "" : storedType);
                    legacy.setType("LEGACY");
                    legacy.setDescription(storedType == null ? "Legacy product" : storedType.replace('-', ' '));
                    return legacy;
                });
    }

    private boolean isManagedFuneralPackageProduct(String productId) {
        if (productId == null || productId.isBlank()) return false;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM funeral_package WHERE product_id = ?", Integer.class, productId);
            return count != null && count > 0;
        } catch (Exception ignored) {
            return false;
        }
    }


    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private void writeProductAudit(String productId, String action, String oldValue, String newValue, String userId) {
        try {
            jdbcTemplate.update("INSERT INTO product_audit_history (id, product_id, action, old_value, new_value, created_at, created_by) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP,?)",
                    UUID.randomUUID().toString().replace("-", ""), productId, action, oldValue, newValue, userId);
        } catch (Exception ignored) {
            // Product audit table may not exist until the database migration has run.
        }
    }

    private Specification<ProductEntity> findByCriteria(ProductQueryDto productQuery) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (productQuery.getCode() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("code"), productQuery.getCode()));
            }
            if (productQuery.getType() != null && !productQuery.getType().isBlank()) {
                String type = ProductTypeCode.find(productQuery.getType())
                        .map(ProductTypeCode::getCode)
                        .orElse(productQuery.getType().trim().toUpperCase());
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }
            if (productQuery.getCategory() != null && !productQuery.getCategory().isBlank()) {
                Set<String> categoryIds = productClassificationService.resolveCategoryTreeIds(productQuery.getCategory());
                if (categoryIds.isEmpty()) {
                    predicate = cb.and(predicate, cb.disjunction());
                } else {
                    predicate = cb.and(predicate, root.get("categoryId").in(categoryIds));
                }
            }
            if (productQuery.getAvailableForSale() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("availableForSale"), productQuery.getAvailableForSale()));
            }
            if (Boolean.TRUE.equals(productQuery.getStockControlled())) {
                predicate = cb.and(predicate, root.get("type").in(
                        ProductTypeCode.PHYSICAL_PRODUCT.getCode(),
                        ProductTypeCode.CONSUMABLE.getCode(),
                        ProductTypeCode.TOMBSTONE.getCode()));
            } else if (Boolean.FALSE.equals(productQuery.getStockControlled())) {
                predicate = cb.and(predicate, cb.not(root.get("type").in(
                        ProductTypeCode.PHYSICAL_PRODUCT.getCode(),
                        ProductTypeCode.CONSUMABLE.getCode(),
                        ProductTypeCode.TOMBSTONE.getCode())));
            }
            if (productQuery.getDescription() != null && !productQuery.getDescription().isBlank()) {
                String search = "%" + productQuery.getDescription().trim().toUpperCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.upper(root.get("code")), search),
                        cb.like(cb.upper(root.get("description")), search)
                ));
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
