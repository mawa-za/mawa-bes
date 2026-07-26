package za.co.mawa.bes.service.v2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.product.classification.ProductCategoryCreateRequestDto;
import za.co.mawa.bes.dto.product.classification.ProductCategoryDefinitionDto;
import za.co.mawa.bes.dto.product.classification.ProductCategoryUpdateRequestDto;
import za.co.mawa.bes.dto.product.classification.ProductTypeDefinitionDto;
import za.co.mawa.bes.entity.product.ProductCategoryMasterEntity;
import za.co.mawa.bes.enums.ProductTypeCode;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.repository.product.ProductCategoryMasterRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductClassificationService {
    private final ProductCategoryMasterRepository categoryRepository;
    private final ProductRepository productRepository;

    public ProductClassificationService(ProductCategoryMasterRepository categoryRepository,
                                        ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<ProductTypeDefinitionDto> getProductTypes() {
        return ProductTypeCode.definitions();
    }

    public List<ProductCategoryDefinitionDto> getCategories(Boolean activeOnly, String productType) {
        String typeFilter = productType == null || productType.isBlank()
                ? null
                : ProductTypeCode.requireSelectable(productType).getCode();
        List<ProductCategoryMasterEntity> all = categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        Map<String, ProductCategoryMasterEntity> byId = new HashMap<>();
        for (ProductCategoryMasterEntity category : all) {
            byId.put(category.getId(), category);
        }

        List<ProductCategoryDefinitionDto> result = new ArrayList<>();
        for (ProductCategoryMasterEntity category : all) {
            if (Boolean.TRUE.equals(activeOnly) && !Boolean.TRUE.equals(category.getActive())) {
                continue;
            }
            String effectiveType = effectiveProductType(category, byId);
            if (typeFilter != null && effectiveType != null && !typeFilter.equals(effectiveType)) {
                continue;
            }
            result.add(toDto(category, byId));
        }
        result.sort((left, right) -> {
            int path = left.getFullPath().compareToIgnoreCase(right.getFullPath());
            return path != 0 ? path : Integer.compare(left.getSortOrder(), right.getSortOrder());
        });
        return result;
    }

    public ProductCategoryDefinitionDto getCategory(String id) {
        ProductCategoryMasterEntity category = requireCategory(id);
        List<ProductCategoryMasterEntity> all = categoryRepository.findAll();
        Map<String, ProductCategoryMasterEntity> byId = new HashMap<>();
        all.forEach(item -> byId.put(item.getId(), item));
        return toDto(category, byId);
    }

    @Transactional
    public ProductCategoryDefinitionDto create(ProductCategoryCreateRequestDto request, String userId) {
        String code = normaliseCode(request.getCode(), request.getName());
        if (categoryRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new IllegalArgumentException("A product category with code " + code + " already exists.");
        }
        ProductCategoryMasterEntity parent = resolveParent(request.getParentId(), null);
        String productType = resolveProductType(request.getProductType(), parent);
        LocalDateTime now = LocalDateTime.now();
        ProductCategoryMasterEntity category = ProductCategoryMasterEntity.builder()
                .id(UUID.randomUUID().toString())
                .code(code)
                .name(requireName(request.getName()))
                .description(trimToNull(request.getDescription()))
                .parentId(parent == null ? null : parent.getId())
                .productType(productType)
                .active(request.getActive() == null || request.getActive())
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .createdAt(now)
                .createdBy(userId)
                .updatedAt(now)
                .updatedBy(userId)
                .build();
        categoryRepository.save(category);
        return getCategory(category.getId());
    }

    @Transactional
    public ProductCategoryDefinitionDto update(String id, ProductCategoryUpdateRequestDto request, String userId) {
        ProductCategoryMasterEntity category = requireCategory(id);
        String code = normaliseCode(request.getCode(), request.getName());
        if (categoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("A product category with code " + code + " already exists.");
        }
        ProductCategoryMasterEntity parent = resolveParent(request.getParentId(), id);
        if (parent != null && isDescendant(parent.getId(), id)) {
            throw new IllegalArgumentException("A category cannot be moved below one of its own child categories.");
        }
        category.setCode(code);
        category.setName(requireName(request.getName()));
        category.setDescription(trimToNull(request.getDescription()));
        category.setParentId(parent == null ? null : parent.getId());
        String productType = resolveProductType(request.getProductType(), parent);
        validateSubtreeCompatibility(id, productType);
        category.setProductType(productType);
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        category.setUpdatedAt(LocalDateTime.now());
        category.setUpdatedBy(userId);
        categoryRepository.save(category);
        return getCategory(id);
    }

    @Transactional
    public void deactivate(String id, String userId) {
        ProductCategoryMasterEntity category = requireCategory(id);
        if (!categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAscNameAsc(id).isEmpty()) {
            throw new IllegalArgumentException("Deactivate child categories before deactivating this category.");
        }
        category.setActive(false);
        category.setUpdatedAt(LocalDateTime.now());
        category.setUpdatedBy(userId);
        categoryRepository.save(category);
    }

    public Set<String> resolveCategoryTreeIds(String idOrCode) {
        String rootId = resolveCategoryId(idOrCode);
        if (rootId == null || rootId.isBlank() || categoryRepository.findById(rootId).isEmpty()) {
            return Set.of();
        }
        return categoryTreeIds(rootId);
    }

    public String resolveCategoryId(String idOrCode) {
        if (idOrCode == null || idOrCode.isBlank()) {
            return null;
        }
        return categoryRepository.findById(idOrCode.trim())
                .or(() -> categoryRepository.findByCodeIgnoreCase(idOrCode.trim()))
                .map(ProductCategoryMasterEntity::getId)
                .orElse(idOrCode.trim());
    }

    public ProductCategoryMasterEntity requireActiveCategory(String categoryId, String productType) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Product category is required.");
        }
        ProductCategoryMasterEntity category = categoryRepository.findById(categoryId)
                .or(() -> categoryRepository.findByCodeIgnoreCase(categoryId))
                .orElseThrow(() -> new IllegalArgumentException("Product category was not found."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalArgumentException("The selected product category is inactive.");
        }
        String normalisedType = ProductTypeCode.requireSelectable(productType).getCode();
        Map<String, ProductCategoryMasterEntity> byId = new HashMap<>();
        categoryRepository.findAll().forEach(item -> byId.put(item.getId(), item));
        String effectiveType = effectiveProductType(category, byId);
        if (effectiveType != null && !normalisedType.equals(effectiveType)) {
            throw new IllegalArgumentException("The selected category is not available for product type " + normalisedType + ".");
        }
        return category;
    }

    public ProductCategoryDefinitionDto toDto(ProductCategoryMasterEntity category) {
        Map<String, ProductCategoryMasterEntity> byId = new HashMap<>();
        categoryRepository.findAll().forEach(item -> byId.put(item.getId(), item));
        return toDto(category, byId);
    }

    private ProductCategoryDefinitionDto toDto(ProductCategoryMasterEntity category,
                                               Map<String, ProductCategoryMasterEntity> byId) {
        ProductCategoryMasterEntity parent = category.getParentId() == null ? null : byId.get(category.getParentId());
        return ProductCategoryDefinitionDto.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParentId())
                .parentCode(parent == null ? null : parent.getCode())
                .parentName(parent == null ? null : parent.getName())
                .productType(effectiveProductType(category, byId))
                .fullPath(buildPath(category, byId, new HashSet<>()))
                .active(Boolean.TRUE.equals(category.getActive()))
                .sortOrder(category.getSortOrder() == null ? 0 : category.getSortOrder())
                .build();
    }

    private String buildPath(ProductCategoryMasterEntity category,
                             Map<String, ProductCategoryMasterEntity> byId,
                             Set<String> visited) {
        if (!visited.add(category.getId())) {
            return category.getName();
        }
        ProductCategoryMasterEntity parent = category.getParentId() == null ? null : byId.get(category.getParentId());
        if (parent == null) {
            return category.getName();
        }
        return buildPath(parent, byId, visited) + " / " + category.getName();
    }

    private ProductCategoryMasterEntity resolveParent(String parentId, String currentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        if (parentId.equals(currentId)) {
            throw new IllegalArgumentException("A category cannot be its own parent.");
        }
        return requireCategory(parentId);
    }

    private String resolveProductType(String requestedType, ProductCategoryMasterEntity parent) {
        String type = requestedType == null || requestedType.isBlank()
                ? null
                : ProductTypeCode.requireSelectable(requestedType).getCode();
        String parentType = null;
        if (parent != null) {
            Map<String, ProductCategoryMasterEntity> byId = new HashMap<>();
            categoryRepository.findAll().forEach(item -> byId.put(item.getId(), item));
            parentType = effectiveProductType(parent, byId);
            if (type == null) {
                type = parentType;
            }
        }
        if (parentType != null && type != null && !parentType.equals(type)) {
            throw new IllegalArgumentException("A child category must use the same product type as its parent category.");
        }
        return type;
    }

    private void validateSubtreeCompatibility(String categoryId, String proposedType) {
        if (proposedType == null) {
            return;
        }
        Set<String> categoryIds = categoryTreeIds(categoryId);
        for (String id : categoryIds) {
            if (categoryId.equals(id)) {
                continue;
            }
            ProductCategoryMasterEntity descendant = categoryRepository.findById(id).orElse(null);
            if (descendant != null && descendant.getProductType() != null
                    && !proposedType.equals(descendant.getProductType())) {
                throw new IllegalArgumentException("The category type conflicts with one or more child categories.");
            }
        }
        if (productRepository.countByCategoryIdInAndTypeNot(categoryIds, proposedType) > 0) {
            throw new IllegalArgumentException("The category type conflicts with products already assigned to this category hierarchy.");
        }
    }

    private Set<String> categoryTreeIds(String rootId) {
        Set<String> ids = new HashSet<>();
        List<String> queue = new ArrayList<>();
        queue.add(rootId);
        for (int index = 0; index < queue.size(); index++) {
            String categoryId = queue.get(index);
            if (!ids.add(categoryId)) {
                continue;
            }
            for (ProductCategoryMasterEntity child : categoryRepository.findByParentIdOrderBySortOrderAscNameAsc(categoryId)) {
                queue.add(child.getId());
            }
        }
        return ids;
    }

    private String effectiveProductType(ProductCategoryMasterEntity category,
                                        Map<String, ProductCategoryMasterEntity> byId) {
        String effectiveType = null;
        Set<String> visited = new HashSet<>();
        ProductCategoryMasterEntity cursor = category;
        while (cursor != null && visited.add(cursor.getId())) {
            if (cursor.getProductType() != null && !cursor.getProductType().isBlank()) {
                String currentType = ProductTypeCode.requireSelectable(cursor.getProductType()).getCode();
                if (effectiveType != null && !effectiveType.equals(currentType)) {
                    throw new IllegalArgumentException("Product category hierarchy contains conflicting product types.");
                }
                effectiveType = currentType;
            }
            cursor = cursor.getParentId() == null ? null : byId.get(cursor.getParentId());
        }
        return effectiveType;
    }

    private boolean isDescendant(String candidateId, String currentId) {
        Set<String> visited = new HashSet<>();
        String cursor = candidateId;
        while (cursor != null && visited.add(cursor)) {
            if (currentId.equals(cursor)) {
                return true;
            }
            ProductCategoryMasterEntity entity = categoryRepository.findById(cursor).orElse(null);
            cursor = entity == null ? null : entity.getParentId();
        }
        return false;
    }

    private ProductCategoryMasterEntity requireCategory(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product category was not found."));
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product category name is required.");
        }
        String value = name.trim();
        if (value.length() > 160) {
            throw new IllegalArgumentException("Product category name may not exceed 160 characters.");
        }
        return value;
    }

    private String normaliseCode(String code, String name) {
        String value = code == null || code.isBlank() ? name : code;
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Product category code is required.");
        }
        String normalised = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalised.length() > 80) {
            throw new IllegalArgumentException("Product category code may not exceed 80 characters.");
        }
        return normalised;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 500) {
            throw new IllegalArgumentException("Product category description may not exceed 500 characters.");
        }
        return trimmed;
    }
}
