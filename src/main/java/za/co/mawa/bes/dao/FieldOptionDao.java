package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.FieldOptionDto;

import java.util.List;

public interface FieldOptionDao {
    void create(FieldOptionDto fieldOptionDto);
    List<FieldOptionDto> getFieldOptions(String field);
}
