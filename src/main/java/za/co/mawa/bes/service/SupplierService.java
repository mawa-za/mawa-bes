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
                        partnerService.addRole(supplierDto.getPartnerId(), RoleType.SUPPLIER);
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
                }else {
                    throw new PartnerNotFound("Partner not found");
                }


            } else {
                throw new Exception();
            }
        }


        return assign;
    }

    @Override
    public SupplierDto getSupplier(String id) throws PartnerNotFound {

        PartnerDto partnerDto = partnerService.get(id);

        return null;
    }
}
