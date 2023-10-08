package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.RoleDto;
import za.co.mawa.bes.dto.RoleWorkcenterCreateDto;
import za.co.mawa.bes.dto.RoleWorkcenterDto;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.entity.RoleWorkcenterPKEntity;
import za.co.mawa.bes.exception.RoleDoesNotExist;

import java.util.List;

public interface RoleDao {
    void create(RoleDto roleDto) throws Exception;
    List<RoleDto> getAll();
    List<RoleWorkcenterDto> getRoleWorkcenters(String role) throws RoleDoesNotExist;
    void addWorkcenter(RoleWorkcenterCreateDto roleWorkcenterCreateDto ) throws Exception;
    boolean deleteWorkcenter(RoleWorkcenterPKEntity entity) throws Exception;
    boolean deleteRole(String role) throws Exception;
}
