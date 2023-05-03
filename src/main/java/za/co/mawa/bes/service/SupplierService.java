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
    public String createSupplier(SupplierDto supplierDto) throws Exception {
        UserRoleDto userRoleDto = new UserRoleDto();
        if (supplierDto.getUsername() != null) {
            UserDto userDto = userService.getUserByName(supplierDto.getUsername());

            if (userDto.getId() != null) {
                PartnerDto partnerDto = partnerService.getOptional(userDto.getId());
                if (partnerDto != null) {

                    userRoleDto.setUser(supplierDto.getUsername());
                    userRoleDto.setRole(RoleType.SUPPLIER);
                    userService.addRole(userRoleDto);
                    if (supplierDto.getAddressDto() != null) {
                        partnerService.addAddress(supplierDto.getAddressDto());
                    }
                    if (supplierDto.getContactDto() != null) {
                        partnerService.addContact(supplierDto.getContactDto());
                    }
                    if (supplierDto.getIdentityDto() != null) {
                        partnerService.addIdentity(supplierDto.getIdentityDto());
                    }
                }
            }
        }

        return supplierDto.getUsername();
    }

    @Override
    public SupplierDto getSupplier(String id) throws PartnerNotFound {

       PartnerDto partnerDto =  partnerService.get(id);

        return null;
    }
}
