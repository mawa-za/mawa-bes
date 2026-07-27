package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.FieldOptionDao;
import za.co.mawa.bes.dto.FieldCreateDto;
import za.co.mawa.bes.dto.FieldDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.entity.FieldEntity;
import za.co.mawa.bes.entity.FieldOptionEntity;
import za.co.mawa.bes.entity.FieldOptionPKEntity;
import za.co.mawa.bes.enums.ProductTypeCode;
import za.co.mawa.bes.exception.FieldDoesNotExist;
import za.co.mawa.bes.repository.FieldOptionRepository;
import za.co.mawa.bes.repository.FieldRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class FieldOptionService implements FieldOptionDao {
    private static final String TENANT_TYPE = "TENANT";

    @Autowired
    FieldOptionRepository fieldOptionRepository;

    @Autowired
    FieldRepository fieldRepository;

    @Override
    @Transactional
    public void create(FieldOptionDto fieldOptionDto) throws FieldDoesNotExist {
        String field = normalizeCode(fieldOptionDto.getField(), "Field");
        validateTenantMaintainableField(field);

        String description = normalizeDescription(fieldOptionDto.getDescription());
        String code = codeFromDescription(description);

        boolean fieldExists = getFields().stream()
                .anyMatch(existing -> existing.getCode() != null
                        && existing.getCode().equalsIgnoreCase(field));
        if (!fieldExists) {
            throw new FieldDoesNotExist();
        }

        FieldOptionPKEntity key = FieldOptionPKEntity.builder()
                .field(field)
                .code(code)
                .type(TENANT_TYPE)
                .build();

        FieldOptionEntity entity = fieldOptionRepository.findById(key)
                .orElseGet(() -> FieldOptionEntity.builder()
                        .fieldOptionPKEntity(key)
                        .build());
        entity.setDescription(description);
        entity.setValidFrom(new Date());
        entity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
        fieldOptionRepository.save(entity);
    }

    @Transactional
    public FieldOptionDto update(String field, String existingCode, FieldOptionDto request) {
        String normalizedField = normalizeCode(field, "Field");
        String normalizedCode = normalizeCode(existingCode, "Field option code");
        validateTenantMaintainableField(normalizedField);

        FieldOptionPKEntity key = FieldOptionPKEntity.builder()
                .field(normalizedField)
                .code(normalizedCode)
                .type(TENANT_TYPE)
                .build();
        FieldOptionEntity entity = fieldOptionRepository.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Tenant field option not found."));

        entity.setDescription(normalizeDescription(request.getDescription()));
        FieldOptionEntity saved = fieldOptionRepository.save(entity);
        return entityToDto(saved);
    }

    @Override
    public List<FieldOptionDto> getFieldOptions(String field) {
        if ("PRODUCT-TYPE".equalsIgnoreCase(field)) {
            return ProductTypeCode.definitions().stream().map(type -> {
                FieldOptionDto option = new FieldOptionDto();
                option.setField("PRODUCT-TYPE");
                option.setCode(type.getCode());
                option.setType("SYSTEM");
                option.setDescription(type.getName());
                option.setValidFrom(new Date());
                option.setValidTo(Conversion.stringToDate(Constant.END_DATE));
                return option;
            }).toList();
        }
        List<FieldOptionDto> fieldOptionDtoList = new ArrayList<>();
        for (FieldOptionEntity fieldOptionEntity : fieldOptionRepository.findFieldOptions(field)) {
            if (fieldOptionEntity.getValidTo() != null && fieldOptionEntity.getValidTo().after(new Date())) {
                fieldOptionDtoList.add(entityToDto(fieldOptionEntity));
            }
        }
        return fieldOptionDtoList;
    }

    public List<FieldOptionDto> getAllFieldOptions() {
        List<FieldOptionDto> fieldOptionDtoList = new ArrayList<>();

        Date today = new Date();
        for (FieldOptionEntity option : fieldOptionRepository.findAll()) {
            String field = option.getFieldOptionPKEntity().getField();
            if ("PRODUCT-TYPE".equalsIgnoreCase(field) || "PRODUCT-CATEGORY".equalsIgnoreCase(field)) {
                continue;
            }
            if (option.getValidTo() == null || !option.getValidTo().after(today)) {
                continue;
            }
            fieldOptionDtoList.add(entityToDto(option));
        }

        return fieldOptionDtoList;
    }

    @Override
    public List<FieldDto> getFields() {
        List<FieldDto> fieldDtoList = new ArrayList<>();
        List<FieldEntity> fieldEntities = fieldRepository.findAll();
        for (FieldEntity fieldEntity : fieldEntities) {
            if ("PRODUCT-TYPE".equalsIgnoreCase(fieldEntity.getCode())
                    || "PRODUCT-CATEGORY".equalsIgnoreCase(fieldEntity.getCode())) {
                continue;
            }
            fieldDtoList.add(new FieldDto(
                    fieldEntity.getCode(),
                    fieldEntity.getDescription(),
                    fieldEntity.getValidFrom(),
                    fieldEntity.getValidTo()
            ));
        }
        return fieldDtoList;
    }

    @Override
    public String getFieldOptionDescription(String field, String code) {
        return getFieldOptions(field).stream()
                .filter(option -> Objects.equals(option.getCode(), code))
                .map(FieldOptionDto::getDescription)
                .findFirst()
                .orElse(null);
    }

    public FieldOptionDto getFieldOption(String field, String code) {
        return getFieldOptions(field).stream()
                .filter(option -> Objects.equals(option.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public FieldOptionDto getOption(String code) {
        try {
            return fieldOptionRepository.findFieldOption(code).stream()
                    .findFirst()
                    .map(this::entityToDto)
                    .orElseGet(FieldOptionDto::new);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getOptionalFieldDescription(String field, String code) {
        return getFieldOptionDescription(field, code);
    }

    @Override
    @Transactional
    public FieldDto createField(FieldCreateDto field) {
        try {
            String description = normalizeDescription(field.getDescription());
            String code = codeFromDescription(description);
            FieldEntity entity = fieldRepository.findById(code).orElseGet(FieldEntity::new);
            entity.setDescription(description);
            entity.setCode(code);
            entity.setValidTo(hasText(field.getValidTo()) ? field.getValidTo() : "9999-12-31");
            entity.setValidFrom(hasText(field.getValidFrom())
                    ? field.getValidFrom()
                    : Conversion.dateToString(new Date()));
            return entityFieldToDto(fieldRepository.save(entity));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @Transactional
    public void deleteFieldOption(String field, String option) {
        String normalizedField = normalizeCode(field, "Field");
        String normalizedOption = normalizeCode(option, "Field option code");
        validateTenantMaintainableField(normalizedField);

        FieldOptionPKEntity key = FieldOptionPKEntity.builder()
                .code(normalizedOption)
                .field(normalizedField)
                .type(TENANT_TYPE)
                .build();
        FieldOptionEntity fieldOption = fieldOptionRepository.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Tenant field option not found."));
        fieldOption.setValidTo(new Date());
        fieldOptionRepository.save(fieldOption);
    }

    static String codeFromDescription(String description) {
        return normalizeDescription(description)
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "-");
    }

    private static String normalizeCode(String value, String label) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    private static String normalizeDescription(String description) {
        if (!hasText(description)) {
            throw new IllegalArgumentException("Description is required.");
        }
        return description.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static void validateTenantMaintainableField(String field) {
        if ("PRODUCT-TYPE".equalsIgnoreCase(field)) {
            throw new IllegalArgumentException("Product types are system controlled and cannot be maintained by a tenant.");
        }
        if ("PRODUCT-CATEGORY".equalsIgnoreCase(field)) {
            throw new IllegalArgumentException("Maintain product categories through Product Maintenance so hierarchy and product rules remain consistent.");
        }
    }

    private FieldOptionDto entityToDto(FieldOptionEntity entity) {
        FieldOptionDto dto = new FieldOptionDto();
        dto.setField(entity.getFieldOptionPKEntity().getField());
        dto.setCode(entity.getFieldOptionPKEntity().getCode());
        dto.setType(entity.getFieldOptionPKEntity().getType());
        dto.setDescription(entity.getDescription());
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidTo(entity.getValidTo());
        return dto;
    }

    private FieldDto entityFieldToDto(FieldEntity entity) {
        FieldDto dto = new FieldDto();
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidTo(entity.getValidTo());
        return dto;
    }
}
