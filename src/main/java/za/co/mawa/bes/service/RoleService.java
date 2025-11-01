package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.RoleDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.exception.RoleDoesNotExist;
import za.co.mawa.bes.repository.RoleRepository;
import za.co.mawa.bes.repository.RoleWorkcenterRepository;
import za.co.mawa.bes.repository.UserRoleRepository;
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
    @Autowired
    UserService userService;
    @Autowired
    UserRoleRepository userRoleRepository;

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

    public RoleOutboundDto get(String id) {
        RoleEntity roleEntity = roleRepository.getById(id);
        RoleOutboundDto roleOutboundDto = new RoleOutboundDto();
        roleOutboundDto.setId(roleEntity.getId());
        roleOutboundDto.setName(roleEntity.getDescription());
        roleOutboundDto.setDescription(roleEntity.getDescription());
        roleOutboundDto.setValidFrom(roleEntity.getValidFrom());
        roleOutboundDto.setValidTo(roleEntity.getValidTo());
        return roleOutboundDto;
    }

    @Override
    public List<RoleWorkcenterDto> getRoleWorkcenters(String role) throws RoleDoesNotExist {
        if (role.equals("SYSTEM")) {
            int i = 0;
            List<RoleWorkcenterDto> roleWorkcenterDtoList = new ArrayList<>();
            List<WorkcenterDto> workcenterDtoList = workcenterService.getAll();
            for (WorkcenterDto workcenterDto : workcenterDtoList) {
                RoleWorkcenterDto roleWorkcenterDto = new RoleWorkcenterDto();
                roleWorkcenterDto.setPosition(i);
                roleWorkcenterDto.setWorkcenter(workcenterDto);
                roleWorkcenterDtoList.add(roleWorkcenterDto);
                i++;
            }
            return roleWorkcenterDtoList;
        } else {
            List<RoleWorkcenterDto> workcenterDtoList = new ArrayList<>();
            List<RoleWorkcenterEntity> roleWorkcenterEntities = roleWorkcenterRepository.findRoleWorkcenters(role);
            for (RoleWorkcenterEntity roleWorkcenterEntity : roleWorkcenterEntities) {
                RoleWorkcenterDto roleWorkcenterDto = new RoleWorkcenterDto();
                String workcenter = roleWorkcenterEntity.getRoleWorkcenterPKEntity().getWorkcenter();
                WorkcenterDto workcenterDto = workcenterService.getById(workcenter);
                roleWorkcenterDto.setPosition(roleWorkcenterEntity.getPosition());
                roleWorkcenterDto.setWorkcenter(workcenterDto);
                workcenterDtoList.add(roleWorkcenterDto);
            }
            return workcenterDtoList;
        }
    }

    @Override
    public void addWorkcenter(RoleWorkcenterCreateDto roleWorkcenterCreateDto) throws Exception {
        try {
            RoleWorkcenterPKEntity roleWorkcenterPKEntity = new RoleWorkcenterPKEntity();
            roleWorkcenterPKEntity.setRole(roleWorkcenterCreateDto.getRole());
            roleWorkcenterPKEntity.setWorkcenter(roleWorkcenterCreateDto.getWorkcenter());
            RoleWorkcenterEntity roleWorkcenterEntity = new RoleWorkcenterEntity();
            roleWorkcenterEntity.setRoleWorkcenterPKEntity(roleWorkcenterPKEntity);
            roleWorkcenterEntity.setPosition(roleWorkcenterCreateDto.getPosition());
            roleWorkcenterRepository.save(roleWorkcenterEntity);
        } catch (Exception exception) {
            throw new Exception("Failed to add role:" + roleWorkcenterCreateDto.toString());
        }
    }

    @Override
    public boolean deleteWorkcenter(RoleWorkcenterPKEntity entity) throws Exception {
        try {
            roleWorkcenterRepository.deleteById(entity);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean deleteRole(String role) throws Exception {
        try {
            roleRepository.deleteById(role);
            for (RoleWorkcenterDto roleWorkcenterDto : getRoleWorkcenters(role)) {
                RoleWorkcenterPKEntity entity = new RoleWorkcenterPKEntity();
                entity.setRole(role);
                entity.setWorkcenter(roleWorkcenterDto.getWorkcenter().getId());
                deleteWorkcenter(entity);
            }
            for (UserRoleEntity userRole : userRoleRepository.findRoles(role)) {
                userService.deleteRole(userRole.getUserRolePKEntity());
            }
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
