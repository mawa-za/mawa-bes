package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.FieldOptionDao;
import za.co.mawa.bes.dto.FieldCreateDto;
import za.co.mawa.bes.dto.FieldDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.entity.FieldEntity;
import za.co.mawa.bes.entity.FieldOptionEntity;
import za.co.mawa.bes.entity.FieldOptionPKEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.exception.FieldDoesNotExist;
import za.co.mawa.bes.enums.ProductTypeCode;
import za.co.mawa.bes.repository.FieldOptionRepository;
import za.co.mawa.bes.repository.FieldRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;

import java.util.*;

@Service
public class FieldOptionService implements FieldOptionDao {
    @Autowired
    FieldOptionRepository fieldOptionRepository;

    @Autowired
    FieldRepository fieldRepository;

    @Override
    public void create(FieldOptionDto fieldOptionDto) throws FieldDoesNotExist {
        if ("PRODUCT-TYPE".equalsIgnoreCase(fieldOptionDto.getField())) {
            throw new IllegalArgumentException("Product types are system controlled and cannot be added or changed by a tenant.");
        }
        if ("PRODUCT-CATEGORY".equalsIgnoreCase(fieldOptionDto.getField())) {
            throw new IllegalArgumentException("Maintain product categories through Product Maintenance so hierarchy and product rules remain consistent.");
        }
        List<FieldDto> result = getFields().stream()
                .filter(a -> Objects.equals(a.getCode(), fieldOptionDto.getField()))
                .toList();
        if (!result.isEmpty()) {
            fieldOptionDto.setValidFrom(new Date());
            fieldOptionDto.setType("TENANT");
            FieldOptionPKEntity fieldOptionPKEntity = new FieldOptionPKEntity();
            fieldOptionPKEntity.setField(fieldOptionDto.getField());
            fieldOptionPKEntity.setType(fieldOptionDto.getType());
            fieldOptionPKEntity.setCode(fieldOptionDto.getCode());
            if (fieldOptionRepository.existsById(fieldOptionPKEntity)) {
                FieldOptionEntity fieldOptionEntity = fieldOptionRepository.getById(fieldOptionPKEntity);
                fieldOptionEntity.setValidFrom(new Date());
                fieldOptionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
                fieldOptionRepository.save(fieldOptionEntity);
            } else {
                FieldOptionEntity fieldOptionEntity = dtoToEntity(fieldOptionDto);
                fieldOptionRepository.save(fieldOptionEntity);
            }
        } else {
            throw new FieldDoesNotExist();
        }

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
            if (fieldOptionEntity.getValidTo().after(new Date())) {
                fieldOptionDtoList.add(entityToDto(fieldOptionEntity));
            }
        }
        return fieldOptionDtoList;
    }

    public List<FieldOptionDto> getAllFieldOptions() {
        List<FieldOptionDto> fieldOptionDtoList = new ArrayList<>();

        for (FieldOptionEntity option : fieldOptionRepository.findAll()) {
            String field = option.getFieldOptionPKEntity().getField();
            if ("PRODUCT-TYPE".equalsIgnoreCase(field) || "PRODUCT-CATEGORY".equalsIgnoreCase(field)) {
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
            FieldDto fieldDto = new FieldDto();
            fieldDto.setCode(fieldEntity.getCode());
            fieldDto.setDescription(fieldEntity.getDescription());
            fieldDto.setValidFrom(fieldEntity.getValidFrom());
            fieldDto.setValidTo(fieldEntity.getValidTo());
            fieldDtoList.add(new FieldDto(fieldDto.getCode(), fieldDto.getDescription(), fieldDto.getValidFrom(), fieldDto.getValidTo()));
        }
        return fieldDtoList;
    }


    @Override
    public String getFieldOptionDescription(String field, String code) {
        List<FieldOptionDto> fieldOptionDtoList = getFieldOptions(field).stream()
                .filter(a -> Objects.equals(a.getCode(), code))
                .toList();
        if (!fieldOptionDtoList.isEmpty()) {
            return fieldOptionDtoList.iterator().next().getDescription();
        } else {
            return null;
        }
    }

    public FieldOptionDto getFieldOption(String field, String code) {
        List<FieldOptionDto> fieldOptionDtoList = getFieldOptions(field).stream()
                .filter(a -> Objects.equals(a.getCode(), code))
                .toList();
        if (!fieldOptionDtoList.isEmpty()) {
            return fieldOptionDtoList.iterator().next();
        } else {
            return null;
        }
    }

    public FieldOptionDto getOption(String code){
        FieldOptionDto fieldOptionDto = new FieldOptionDto();
        try {
            List<FieldOptionEntity> fieldOptions = fieldOptionRepository.findFieldOption(code);
            for(FieldOptionEntity fieldOption : fieldOptions) {
                fieldOptionDto.setField(fieldOption.getFieldOptionPKEntity().getField());
                fieldOptionDto.setCode(fieldOption.getFieldOptionPKEntity().getCode());
                fieldOptionDto.setType(fieldOption.getFieldOptionPKEntity().getType());
                fieldOptionDto.setDescription(fieldOption.getDescription());
                fieldOptionDto.setValidFrom(fieldOption.getValidFrom());
                fieldOptionDto.setValidTo(fieldOption.getValidTo());
                return fieldOptionDto;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return fieldOptionDto;
    }

    @Override
    public String getOptionalFieldDescription(String field, String code) {
        return getFieldOptionDescription(field, code);
    }

    @Override
    public FieldDto createField(FieldCreateDto Field) {
        try {
            FieldEntity entity = new FieldEntity();
            entity.setDescription(Field.getDescription());
            String code = Field.getDescription().toUpperCase().replace(" ", "-");
            entity.setCode(code);
            if (Field.getValidTo() != null && Field.getValidTo() != "") {
                entity.setValidTo(Field.getValidTo());
            } else {
                entity.setValidTo("9999-12-31");
            }
            if (Field.getValidFrom() != null && Field.getValidFrom() != "") {
                entity.setValidFrom(Field.getValidFrom());
            } else {
                entity.setValidFrom(Conversion.dateToString(new Date()));
            }
            return entityFieldToDto(fieldRepository.save(entity));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void deleteFieldOption(String field, String option) throws Exception {
        if ("PRODUCT-TYPE".equalsIgnoreCase(field)) {
            throw new IllegalArgumentException("Product types are system controlled and cannot be deleted.");
        }
        if ("PRODUCT-CATEGORY".equalsIgnoreCase(field)) {
            throw new IllegalArgumentException("Maintain product categories through Product Maintenance so hierarchy and product rules remain consistent.");
        }
        try {
            FieldOptionPKEntity pk = new FieldOptionPKEntity();
            pk.setCode(option);
            pk.setField(field);
            pk.setType("TENANT");
            FieldOptionEntity fieldOption = fieldOptionRepository.getById(pk);
            fieldOption.setValidTo(new Date());
            fieldOptionRepository.save(fieldOption);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    private FieldOptionDto entityToDto(FieldOptionEntity fieldOptionEntity) {
        FieldOptionDto fieldOptionDto = new FieldOptionDto();
        fieldOptionDto.setField(fieldOptionEntity.getFieldOptionPKEntity().getField());
        fieldOptionDto.setCode(fieldOptionEntity.getFieldOptionPKEntity().getCode());
        fieldOptionDto.setType(fieldOptionEntity.getFieldOptionPKEntity().getType());
        fieldOptionDto.setDescription(fieldOptionEntity.getDescription());
        fieldOptionDto.setValidFrom(fieldOptionEntity.getValidFrom());
        fieldOptionDto.setValidTo(fieldOptionEntity.getValidTo());
        return fieldOptionDto;
    }

    private FieldDto entityFieldToDto(FieldEntity fieldEntity) {
        FieldDto fieldDto = new FieldDto();
        fieldDto.setCode(fieldEntity.getCode());
        fieldDto.setDescription(fieldEntity.getDescription());
        fieldDto.setValidFrom(fieldEntity.getValidFrom());
        fieldDto.setValidTo(fieldEntity.getValidTo());
        return fieldDto;
    }

    private FieldOptionEntity dtoToEntity(FieldOptionDto fieldOptionDto) {
        FieldOptionPKEntity fieldOptionPKEntity = new FieldOptionPKEntity();
        fieldOptionPKEntity.setField(fieldOptionDto.getField());
        fieldOptionPKEntity.setCode(fieldOptionDto.getCode());
        fieldOptionPKEntity.setType(fieldOptionDto.getType());

        FieldOptionEntity fieldOptionEntity = new FieldOptionEntity();
        fieldOptionEntity.setFieldOptionPKEntity(fieldOptionPKEntity);
        fieldOptionEntity.setDescription(fieldOptionDto.getDescription());
        fieldOptionEntity.setValidFrom(fieldOptionDto.getValidFrom());
        fieldOptionEntity.setValidTo(fieldOptionDto.getValidTo());
        return fieldOptionEntity;
    }
}
