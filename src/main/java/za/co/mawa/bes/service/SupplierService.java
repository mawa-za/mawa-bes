package za.co.mawa.bes.service;

import org.hibernate.usertype.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.SupplierDao;
import za.co.mawa.bes.dto.IdentityDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.SupplierDto;
import za.co.mawa.bes.dto.user.UserCreateDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.dto.user.UserRoleDto;
import za.co.mawa.bes.exception.PartnerNotFound;
import za.co.mawa.bes.utils.IdType;
import za.co.mawa.bes.utils.PartnerType;
import za.co.mawa.bes.utils.RoleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierService implements SupplierDao {

    @Autowired
    UserService userService;
    @Autowired
    PartnerService partnerService;

    @Override
    public boolean assignSupplier(SupplierDto supplierDto) throws Exception {
        boolean assign = false;
        UserRoleDto userRoleDto = new UserRoleDto();
        if (supplierDto.getUsername() != null) {
            UserDto userDto = userService.getUserByName(supplierDto.getUsername());

            if (userDto.getId() != null) {
                try {
                    PartnerDto partnerDto = partnerService.get(userDto.getId());
                    if (partnerDto.getId() != null) {
                        userRoleDto.setUser(supplierDto.getUsername());
                        userRoleDto.setRole(RoleType.SUPPLIER);
                        userService.addRole(userRoleDto);
                        partnerService.addRole(userDto.getId(), RoleType.SUPPLIER);
                        assign = true;
                    }
                } catch (Exception e) {
                    throw new PartnerNotFound("Partner Not found");
                }

            }
        } else {

            if (supplierDto.getPartnerId() != null) {


                PartnerDto partnerDto = partnerService.getOptional(supplierDto.getPartnerId());
                if (partnerDto.getId() != null) {
                    UserDto userDto = userService.getUserByID(partnerDto.getId());
                    if (userDto != null) {
                        userRoleDto.setUser(userDto.getUsername());
                        userRoleDto.setRole(RoleType.SUPPLIER);
                        userService.addRole(userRoleDto);
                        partnerService.addRole(supplierDto.getPartnerId(), RoleType.SUPPLIER);
                        assign = true;
                    }
                } else {
                    throw new PartnerNotFound("Partner not found");
                }


            } else {
                throw new Exception();
            }
        }


        return assign;
    }

    @Override
    public SupplierDto getSupplier(String id) throws Exception {


        SupplierDto supplierDto = Optional.ofNullable(partnerService.get(id))
                .filter(partnerDto -> partnerDto.getId() != null)
                .map(partnerDto -> {
                    ArrayList<String> roles = partnerService.getRoles(partnerDto.getId());
                    return roles.stream()
                            .filter(role -> role.equals(RoleType.SUPPLIER))
                            .findFirst()
                            .map(role -> {
                                UserDto userDto = null;
                                try {
                                    userDto = userService.getUserByID(partnerDto.getId());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return Optional.ofNullable(userDto.getUsername())
                                        .map(username -> {
                                            List<String> userRoles = userService.getRoles(username);
                                            return userRoles.stream()
                                                    .filter(userRole -> userRole.equals(RoleType.SUPPLIER))
                                                    .findFirst()
                                                    .map(userRole -> {
                                                        SupplierDto supplier = new SupplierDto();
                                                        supplier.setPartnerId(partnerDto.getId());
                                                        supplier.setUsername(username);
                                                        supplier.setSupplierType(partnerDto.getType());
                                                        if (partnerDto.getType().equals(PartnerType.ORGANIZATION)) {
                                                            supplier.setOrganizationName(partnerDto.getName1());
                                                        } else {
                                                            supplier.setFirstName(partnerDto.getName2());
                                                            supplier.setMiddleName(partnerDto.getName3());
                                                            supplier.setLastName(partnerDto.getName1());
                                                        }
                                                        return supplier;
                                                    })
                                                    .orElse(null);
                                        })
                                        .orElse(null);
                            })
                            .orElse(null);
                })
                .orElseThrow(() -> new PartnerNotFound("Partner not found"));


        return supplierDto;
    }
}
