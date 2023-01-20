package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.RoleDao;
import za.co.mawa.bes.dto.RoleDto;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.entity.RoleEntity;
import za.co.mawa.bes.entity.UserRoleEntity;
import za.co.mawa.bes.entity.UserRolePKEntity;
import za.co.mawa.bes.repository.RoleRepository;
import za.co.mawa.bes.repository.UserRoleRepository;

import java.util.List;

@Service
@Transactional
public class RoleService implements RoleDao {
    @Autowired
    RoleRepository roleRepository;

    @Override
    public void create(RoleDto roleDto) {
        roleRepository.save(dtoToEntity(roleDto));
    }

    @Override
    public List<WorkcenterDto> getRoleWorkcenters(String role) {
        return null;
    }

    private RoleDto entityToDto(RoleEntity roleEntity) {
        RoleDto roleDto = new RoleDto();
        roleDto.setId(roleEntity.getId());
        roleDto.setDescription(roleEntity.getDescription());
        roleDto.setValidFrom(roleEntity.getValidFrom());
        roleDto.setValidTo(roleEntity.getValidTo());
        return roleDto;
    }

    private RoleEntity dtoToEntity(RoleDto roleDto) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(roleDto.getId());
        roleEntity.setDescription(roleDto.getDescription());
        roleEntity.setValidFrom(roleDto.getValidFrom());
        roleEntity.setValidTo(roleDto.getValidTo());
        return roleEntity;
    }
}
