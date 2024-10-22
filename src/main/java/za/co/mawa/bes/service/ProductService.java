package za.co.mawa.bes.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ProductDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.product.*;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeCreateDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeEditDto;
import za.co.mawa.bes.dto.product.attribute.ProductAttributeQueryDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryCreateDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryDto;
import za.co.mawa.bes.dto.product.category.ProductCategoryProcessDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingCreateDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingEditDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingQueryDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.entity.product.ProductCategoryEntity;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    TransactionAttributeService transactionAttributeService;
    @Autowired
    TransactionAttributeRepository transactionAttributeRepository;


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
            productEntity.setType(productCreateDto.getType().toUpperCase());
//            productEntity.setCategory(productCreateDto.getCategory().toUpperCase());
            productEntity.setValidFrom(new Date());
            productEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            productEntity.setUom(productCreateDto.getBaseUnitOfMeasure().toUpperCase());


            ProductDto productDto = get(productRepository.save(productEntity).getId());

            TransactionAttributeDto transactionAttributeFromDto = new TransactionAttributeDto();
            transactionAttributeFromDto.setTransaction(productEntity.getId());
            transactionAttributeFromDto.setAttribute("VAT-INCLUSIVE");
            transactionAttributeFromDto.setValue(productCreateDto.getVatInclusive());
            transactionAttributeService.add(transactionAttributeFromDto);

            if (productCreateDto.getPricingType() != null && productCreateDto.getPricingType() != "") {
                ProductPricingCreateDto productPricingCreateDto = new ProductPricingCreateDto();
                productPricingCreateDto.setProduct(productDto.getId());
                productPricingCreateDto.setPricing(productCreateDto.getPricingType());
                productPricingCreateDto.setValue(productCreateDto.getPrice());
                productPricingCreateDto.setValidFrom(new Date());
                productPricingCreateDto.setValidTo(Conversion.stringToDate(Constant.END_DATE));
                addPricing(productPricingCreateDto);
            }
            return productDto;
        } catch (Exception exception) {
            throw new ProductCreationFailure();
        }
    }

    @Override
    public List<ProductDto> search(ProductQueryDto productQueryDto) {
        List<ProductDto> productDtoList = new ArrayList<>();
        Sort sort = Sort.by("id").descending();
        List<ProductEntity> productEntityList = productRepository.findAll(findByCriteria(productQueryDto), sort);
        for (ProductEntity productEntity : productEntityList) {
            try {
                productDtoList.add(get(productEntity.getId()));
            } catch (Exception exception) {
            }
        }
        for (ProductCategoryEntity productCategoryEntity : productCategoryRepository.findByCategory(productQueryDto.getCategory())) {
            try {
                List<ProductDto> filteredList = productDtoList.stream()
                        .filter(a -> Objects.equals(a.getId(), productCategoryEntity.getId()))
                        .toList();
                if (!filteredList.isEmpty()) {
                    productDtoList.add(get(productCategoryEntity.getId()));
                }

            } catch (Exception exception) {
            }
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
            productDto.setType(fieldOptionService.getFieldOption(Field.PRODUCT_TYPE, productEntity.getType()));
//            productDto.setCategory(fieldOptionService.getFieldOption(Field.PRODUCT_CATEGORY, productEntity.getCategory()));
            productDto.setBaseUnitOfMeasure(fieldOptionService.getFieldOption(Field.UOM, productEntity.getUom()));
            productDto.setValidTo(productEntity.getValidTo());
            productDto.setValidFrom(productEntity.getValidFrom());
            productDto.setPricings(getPricings(id));
            productDto.setAttributes(getAttributes(id));
            productDto.setCategories(getCategories(id));
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
            productBasicDto.setType(fieldOptionService.getFieldOption(Field.PRODUCT_TYPE, productEntity.getType()));
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
            if (productEditDto.getCode() != null && productEditDto.getCode() != "") {
                productEntity.setCode(productEditDto.getCode());
            }
            if (productEditDto.getCategory() != null && productEditDto.getCategory() != "") {
//                productEntity.setCategory(productEditDto.getCategory());
            }
            if (productEditDto.getDescription() != null && productEditDto.getDescription() != "") {
                productEntity.setDescription(productEditDto.getDescription());
            }
            if (productEditDto.getBaseUnitOfMeasure() != null && productEditDto.getBaseUnitOfMeasure() != "") {
                productEntity.setUom(productEditDto.getBaseUnitOfMeasure().toUpperCase());
            }
            productRepository.save(productEntity);

            if(productEditDto.getVatInclusive() !=null && productEditDto.getVatInclusive()!=""){

                List<TransactionAttributeEntity> attribute = transactionAttributeService.getByTransactionId(productEditDto.getId());
                for (TransactionAttributeEntity attribute1 : attribute) {
                    attribute1.setValue(productEditDto.getVatInclusive());
                    transactionAttributeRepository.save(attribute1);
                }
            }
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
            BigDecimal vatPercentage = new BigDecimal("0.15");

            ProductPricingPKEntity productPricingPKEntity = new ProductPricingPKEntity();
            productPricingPKEntity.setProduct(productPricingQueryDto.getProduct());
            productPricingPKEntity.setPricing(productPricingQueryDto.getPricing());
            ProductPricingEntity productPricingEntity = productPricingRepository.getById(productPricingPKEntity);
            ProductPricingDto productPricingDto = new ProductPricingDto();
            productPricingDto.setPricing(fieldOptionService.getFieldOption(Field.PRODUCT_PRICING, productPricingEntity.getProductPricingPKEntity().getPricing()));
            productPricingDto.setValue(productPricingEntity.getValue());

            BigDecimal value = productPricingEntity.getValue();
            productPricingDto.setValidFrom(productPricingEntity.getValidFrom());
            productPricingDto.setValidTo(productPricingEntity.getValidTo());

            BigDecimal totExcVat = value;
            BigDecimal vatAmount = totExcVat.multiply(vatPercentage);
            BigDecimal totIncVat = totExcVat.add(vatAmount);

            productPricingDto.setTotExcVat(totExcVat);
            productPricingDto.setVatAmount(vatAmount);
            productPricingDto.setTotIncVat(totIncVat);

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
            BigDecimal vatPercentage = new BigDecimal("0.15");

            for (ProductPricingEntity productPricingEntity : productPricingEntityList) {
                ProductPricingDto productPricingDto = new ProductPricingDto();
                BigDecimal value = productPricingEntity.getValue();

                productPricingDto.setProduct(productPricingEntity.getProductPricingPKEntity().getProduct());
                productPricingDto.setPricing(fieldOptionService.getFieldOption(Field.PRICING_TYPE, productPricingEntity.getProductPricingPKEntity().getPricing()));
                productPricingDto.setValue(productPricingEntity.getValue());
                productPricingDto.setValidFrom(productPricingEntity.getValidFrom());
                productPricingDto.setValidTo(productPricingEntity.getValidTo());

                BigDecimal vatAmount = value.multiply(vatPercentage);
                BigDecimal totIncVat = value.add(vatAmount);

//                productPricingDto.setVatInclusive("yes");
                productPricingDto.setTotExcVat(value);
                productPricingDto.setVatAmount(vatAmount);
                productPricingDto.setTotIncVat(totIncVat);

//                List<TransactionAttributeEntity> attribute = transactionAttributeService.getByTransactionId(product);
//                if(attribute != null && attribute.size() > 0) {
//                    for (TransactionAttributeEntity attribute1 : attribute) {
//                        if (attribute1.getValue().equalsIgnoreCase("yes")) {
//                            productPricingDto.setVatInclusive("yes");
//
//                        }else if (attribute1.getValue().equalsIgnoreCase("no")) {
//                            productPricingDto.setVatInclusive("no");
//                            productPricingDto.setTotExcVat(value);
//                            productPricingDto.setVatAmount(BigDecimal.valueOf(0));
//                            productPricingDto.setTotIncVat(value);
//                        }
//                    }
//                }
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

    public void addCategory(ProductCategoryProcessDto productCategoryProcessDto) throws Exception {
        try {
            ProductCategoryEntity productCategoryEntity = new ProductCategoryEntity();
            productCategoryEntity.setProduct(productCategoryProcessDto.getProduct());
            productCategoryEntity.setCategory(productCategoryProcessDto.getCategory());
            productCategoryEntity.setValidFrom(new Date());
            productCategoryEntity.setValidTo(Conversion.stringToDate("9999-12-31"));
            productCategoryRepository.save(productCategoryEntity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ArrayList<ProductCategoryDto> getCategories(String id) {
        try {
            ArrayList<ProductCategoryDto> categoryDtoArrayList = new ArrayList<>();
            for (ProductCategoryEntity productCategoryEntity : productCategoryRepository.findByProduct(id)) {
                ProductCategoryDto productCategoryDto = new ProductCategoryDto();
                productCategoryDto.setCategory(fieldOptionService.getFieldOption(Field.PRODUCT_CATEGORY, productCategoryEntity.getCategory()));
                productCategoryDto.setProduct(productCategoryEntity.getProduct());
                productCategoryDto.setValidFrom(productCategoryEntity.getValidFrom());
                productCategoryDto.setValidTo(productCategoryEntity.getValidTo());
                categoryDtoArrayList.add(productCategoryDto);
            }
            return categoryDtoArrayList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void deleteCategory(ProductCategoryProcessDto productCategoryProcessDto) throws Exception {
        try {
            for(ProductCategoryEntity productCategoryEntity: productCategoryRepository.find(productCategoryProcessDto.getProduct(), productCategoryProcessDto.getCategory())){
                productCategoryRepository.deleteById(productCategoryEntity.getId());
            }
        } catch (Exception e) {

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
            if (productQuery.getType() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), productQuery.getType()));
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
    public PricingOutboundDto simulate(PricingInboundDto pricingInboundDto) {
        List<LineItemOutboundDto> lineItemOutboundDtoList = new ArrayList<>();
        PricingOutboundDto pricingOutboundDto = new PricingOutboundDto();
        pricingOutboundDto.setVATPercentage(new BigDecimal("15"));
        BigDecimal totalExcVat = new BigDecimal("0");
        for (LineItemInboundDto lineItemInboundDto : pricingInboundDto.getItems()) {
            LineItemOutboundDto lineItemOutboundDto = new LineItemOutboundDto();
            lineItemOutboundDto.setTransaction(lineItemInboundDto.getTransaction());
            lineItemOutboundDto.setItem(lineItemInboundDto.getItemId());
            try {
                lineItemOutboundDto.setProduct(get(lineItemInboundDto.getProductId()));
            } catch (ProductNotFoundException e) {

            }
            lineItemOutboundDto.setUnitPrice(lineItemInboundDto.getUnitPrice());
            lineItemOutboundDto.setQuantity(lineItemInboundDto.getQuantity());
            lineItemOutboundDto.setUom(fieldOptionService.getFieldOption(Field.UOM, lineItemInboundDto.getUom()));
            lineItemOutboundDto.setBarcode(lineItemInboundDto.getEan());
            lineItemOutboundDto.setLineTotal(lineItemInboundDto.getQuantity().multiply(lineItemInboundDto.getUnitPrice()));
            totalExcVat = totalExcVat.add(lineItemOutboundDto.getLineTotal());
            lineItemOutboundDtoList.add(lineItemOutboundDto);
        }
        pricingOutboundDto.setTotalExcVat(totalExcVat);
        pricingOutboundDto.setVATAmount(pricingOutboundDto.getTotalExcVat().multiply(pricingOutboundDto.getVATPercentage().divide(new BigDecimal("100"))));
        pricingOutboundDto.setTotalIncVat(pricingOutboundDto.getTotalExcVat().add((pricingOutboundDto.getVATAmount())));
        pricingOutboundDto.setItems(lineItemOutboundDtoList);
        return pricingOutboundDto;
    }
}
