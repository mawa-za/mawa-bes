package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.RoleDao;
import za.co.mawa.bes.dto.RoleDto;
import za.co.mawa.bes.dto.RoleWorkcenterDto;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.entity.RoleEntity;
import za.co.mawa.bes.entity.RoleWorkcenterEntity;
import za.co.mawa.bes.entity.RoleWorkcenterPKEntity;
import za.co.mawa.bes.exception.RoleDoesNotExist;
import za.co.mawa.bes.repository.RoleRepository;
import za.co.mawa.bes.repository.RoleWorkcenterRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RoleService implements RoleDao {
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    RoleWorkcenterRepository roleWorkcenterRepository;
    @Autowired
    WorkcenterService workcenterService;

    @Override
    public void create(RoleDto roleDto) throws Exception {
        try {
            RoleEntity roleEntity = new RoleEntity();
            roleEntity.setId(roleDto.getId());
            roleEntity.setDescription(roleDto.getDescription());
            roleEntity.setValidFrom(new Date());
            roleEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            roleRepository.save(dtoToEntity(roleDto));
        } catch (Exception exception) {
            throw new Exception("Failed to create role: " + roleDto.toString());
        }
    }

    @Override
    public List<RoleDto> getAll() {
        List<RoleDto> roleDtoList = new ArrayList<>();
        for (RoleEntity roleEntity : roleRepository.findAll()) {
            RoleDto roleDto = new RoleDto();
            roleDto.setId(roleEntity.getId());
            roleDto.setDescription(roleEntity.getDescription());
            roleDto.setValidFrom(roleEntity.getValidFrom());
            roleDto.setValidTo(roleEntity.getValidTo());
            roleDtoList.add(roleDto);
        }
        return roleDtoList;
    }

    @Override
    public List<WorkcenterDto> getRoleWorkcenters(String role) throws RoleDoesNotExist {
        List<WorkcenterDto> workcenterDtoList = new ArrayList<>();
        List<RoleWorkcenterEntity> roleWorkcenterEntities = roleWorkcenterRepository.findRoleWorkcenters(role);
        for (RoleWorkcenterEntity roleWorkcenterEntity : roleWorkcenterEntities) {
            String workcenter = roleWorkcenterEntity.getRoleWorkcenterPKEntity().getWorkcenter();
            workcenterDtoList.add(workcenterService.getById(workcenter));
        }
        return workcenterDtoList;
    }

    @Override
    public void addWorkcenter(RoleWorkcenterDto roleWorkcenterDto) throws Exception {
        try {
            RoleWorkcenterPKEntity roleWorkcenterPKEntity = new RoleWorkcenterPKEntity();
            roleWorkcenterPKEntity.setRole(roleWorkcenterDto.getRole());
            roleWorkcenterPKEntity.setWorkcenter(roleWorkcenterDto.getWorkcenter());
            RoleWorkcenterEntity roleWorkcenterEntity = new RoleWorkcenterEntity();
            roleWorkcenterEntity.setRoleWorkcenterPKEntity(roleWorkcenterPKEntity);
            roleWorkcenterEntity.setPosition(roleWorkcenterDto.getPosition());
            roleWorkcenterRepository.save(roleWorkcenterEntity);
        } catch (Exception exception) {
            throw new Exception("Failed to add role:" + roleWorkcenterDto.toString());
        }
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
        return roleEntity;
    }
}
