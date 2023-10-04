package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.FieldCreateDto;
import za.co.mawa.bes.dto.FieldDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.exception.FieldDoesNotExist;

import java.util.List;

public interface FieldOptionDao {
    void create(FieldOptionDto fieldOptionDto) throws FieldDoesNotExist;
    List<FieldOptionDto> getFieldOptions(String field);

    List<FieldDto> getFields();
    String getFieldOptionDescription(String field, String code);

    String getOptionalFieldDescription(String field, String code);
    FieldDto createField(FieldCreateDto Field);
    void deleteFieldOption(String field,String option) throws Exception;
}
