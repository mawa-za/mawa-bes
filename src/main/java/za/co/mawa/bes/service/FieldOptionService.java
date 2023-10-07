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
import za.co.mawa.bes.repository.FieldOptionRepository;
import za.co.mawa.bes.repository.FieldRepository;
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
        List<FieldDto> result = getFields().stream()
                .filter(a -> Objects.equals(a.getCode(), fieldOptionDto.getField()))
                .toList();
        if (!result.isEmpty()) {
            fieldOptionDto.setValidFrom(new Date());
            fieldOptionDto.setType("TENANT");
            fieldOptionRepository.save(dtoToEntity(fieldOptionDto));
        } else {
            throw new FieldDoesNotExist();
        }

    }

    @Override
    public List<FieldOptionDto> getFieldOptions(String field) {
        List<FieldOptionDto> fieldOptionDtos = new ArrayList<>();
        for (FieldOptionEntity fieldOptionEntity : fieldOptionRepository.findFieldOptions(field)) {
            fieldOptionDtos.add(entityToDto(fieldOptionEntity));
        }
        return fieldOptionDtos;
    }

    @Override
    public List<FieldDto> getFields() {
        List<FieldDto> fieldDtoList = new ArrayList<>();
        List<FieldEntity> fieldEntities = fieldRepository.findAll();
        for (FieldEntity fieldEntity : fieldEntities) {
            FieldDto fieldDto = new FieldDto();
            fieldDto.setCode(fieldEntity.getCode());
            fieldDto.setDescription(fieldEntity.getDescription());
            fieldDto.setValidFrom(fieldEntity.getValidFrom());
            fieldDto.setValidTo(fieldEntity.getValidTo());
            fieldDtoList.add(new FieldDto(fieldDto.getCode(), fieldDto.getDescription(),fieldDto.getValidFrom(),fieldDto.getValidTo()));
        }
        return fieldDtoList;
    }


    @Override
    public String getFieldOptionDescription(String field, String code) {
        FieldOptionPKEntity pk = new FieldOptionPKEntity();
        pk.setCode(code);
        pk.setField(field);
        Optional<FieldOptionEntity> fieldOption = fieldOptionRepository.findById(pk);
        if (!fieldOption.isEmpty()) {
            return fieldOption.get().getDescription();
        } else {
            return null;
        }
    }

    @Override
    public String getOptionalFieldDescription(String field, String code) {

        FieldOptionPKEntity pk = new FieldOptionPKEntity();
        pk.setCode(code);
        pk.setField(field);
        Optional<FieldOptionEntity> fieldEntity = fieldOptionRepository.findById(pk);
        FieldOptionEntity fieldOption = fieldEntity.orElse(null);

        if(fieldOption != null)
        {
            return fieldOption.getDescription();
        }
        return null;
    }

    @Override
    public FieldDto createField(FieldCreateDto Field) {
        try{
            FieldEntity entity = new FieldEntity();
            entity.setDescription(Field.getDescription());
            String code = Field.getDescription().toUpperCase().replace(" ","-");
            entity.setCode(code);
            if(Field.getValidTo() != null && Field.getValidTo() != "") {
                entity.setValidTo(Field.getValidTo());
            }
            else{
                entity.setValidTo("9999-12-31");
            }
            if(Field.getValidFrom() != null && Field.getValidFrom() != "") {
                entity.setValidFrom(Field.getValidFrom());
            }
            else{
                entity.setValidFrom(Conversion.dateToString(new Date()));
            }
            return  entityFieldToDto(fieldRepository.save(entity));
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void deleteFieldOption(String field, String option) throws Exception {
        try{
            FieldOptionPKEntity pk = new FieldOptionPKEntity();
            pk.setCode(option);
            pk.setField(field);
            pk.setType("TENANT");
            FieldOptionEntity fieldOption = fieldOptionRepository.getById(pk);
            fieldOption.setValidTo(new Date());
            fieldOptionRepository.save(fieldOption);
        }catch (Exception ex){
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
