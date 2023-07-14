package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.layby.LayByCreateDto;
import za.co.mawa.bes.dto.layby.LayByDto;
import za.co.mawa.bes.dto.layby.LayByEditDto;
import za.co.mawa.bes.dto.layby.LayByQueryDto;
import za.co.mawa.bes.dto.layby.LayByGetDto;

import java.util.ArrayList;

public interface LayByDao {
    String create(LayByCreateDto layByCreateDto) throws Exception;
    LayByDto get(String id) throws Exception;
    ArrayList<LayByGetDto> search(LayByQueryDto queryDto) throws Exception;
    boolean delete(String id) throws Exception;
    boolean edit(String id, LayByEditDto editDto) throws Exception;
}
