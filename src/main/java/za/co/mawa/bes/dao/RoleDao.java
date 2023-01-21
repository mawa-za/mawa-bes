package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.RoleDto;
import za.co.mawa.bes.dto.WorkcenterDto;

import java.util.List;

public interface RoleDao {
    void create(RoleDto roleDto);
    List<WorkcenterDto> getRoleWorkcenters(String role);
}
