package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.FieldOptionDao;
import za.co.mawa.bes.dto.FieldDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.entity.FieldOptionEntity;
import za.co.mawa.bes.entity.FieldOptionPKEntity;
import za.co.mawa.bes.exception.FieldDoesNotExist;
import za.co.mawa.bes.repository.FieldOptionRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class FieldOptionService implements FieldOptionDao {
    @Autowired
    FieldOptionRepository fieldOptionRepository;

    @Override
    public void create(FieldOptionDto fieldOptionDto) throws FieldDoesNotExist {
        List<FieldDto> result = getFields().stream()
                .filter(a -> Objects.equals(a.getId(), fieldOptionDto.getField()))
                .toList();
        if (!result.isEmpty()){
            fieldOptionDto.setValidFrom(new Date());
            fieldOptionRepository.save(dtoToEntity(fieldOptionDto));
        }else{
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
        fieldDtoList.add(new FieldDto("TITLE", "Title"));
        fieldDtoList.add(new FieldDto("GENDER", "Gender"));
        fieldDtoList.add(new FieldDto("IDTYPE", "ID Type"));
        fieldDtoList.add(new FieldDto("USERROLE", "User Role"));
        return fieldDtoList;
    }

    @Override
    public String getFieldOptionDescription(String field, String code) {
        return null;
    }

    private FieldOptionDto entityToDto(FieldOptionEntity fieldOptionEntity) {
        FieldOptionDto fieldOptionDto = new FieldOptionDto();
        fieldOptionDto.setField(fieldOptionEntity.getFieldOptionPKEntity().getField());
        fieldOptionDto.setCode(fieldOptionEntity.getFieldOptionPKEntity().getCode());
        fieldOptionDto.setDescription(fieldOptionEntity.getDescription());
        fieldOptionDto.setValidFrom(fieldOptionEntity.getValidFrom());
        fieldOptionDto.setValidTo(fieldOptionEntity.getValidTo());
        return fieldOptionDto;
    }

    private FieldOptionEntity dtoToEntity(FieldOptionDto fieldOptionDto) {
        FieldOptionPKEntity fieldOptionPKEntity = new FieldOptionPKEntity();
        fieldOptionPKEntity.setField(fieldOptionDto.getField());
        fieldOptionPKEntity.setCode(fieldOptionDto.getCode());

        FieldOptionEntity fieldOptionEntity = new FieldOptionEntity();
        fieldOptionEntity.setFieldOptionPKEntity(fieldOptionPKEntity);
        fieldOptionEntity.setDescription(fieldOptionDto.getDescription());
        fieldOptionEntity.setValidFrom(fieldOptionDto.getValidFrom());
        fieldOptionEntity.setValidTo(fieldOptionDto.getValidTo());
        return fieldOptionEntity;
    }
}
