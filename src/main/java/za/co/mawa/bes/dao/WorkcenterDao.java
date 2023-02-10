package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.exception.RoleDoesNotExist;

import java.util.List;

public interface WorkcenterDao {
    List<WorkcenterDto> getAll();
    WorkcenterDto getById(String id) throws RoleDoesNotExist;
}
