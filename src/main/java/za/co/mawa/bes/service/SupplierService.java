package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.SupplierDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.partner.PartnerQueryDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.dto.user.UserQueryDto;
import za.co.mawa.bes.dto.user.UserRoleDto;
import za.co.mawa.bes.exception.PartnerNotFoundException;
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
    public String assignSupplier(SupplierDto supplierDto) throws Exception {
        String partnerId = "";
        UserRoleDto userRoleDto = new UserRoleDto();
        if (supplierDto.getUsername() != null) {
            UserDto userDto = userService.getUserByName(supplierDto.getUsername());

            if (userDto.getPartner() != null) {
                try {
                    PartnerDto partnerDto = partnerService.get(userDto.getPartner());
                    if (partnerDto.getId() != null) {
                        userRoleDto.setUser(supplierDto.getUsername());
                        userRoleDto.setRole(RoleType.SUPPLIER);
                        userService.addRole(userRoleDto);
                        partnerService.addRole(userDto.getPartner(), RoleType.SUPPLIER);
                        partnerId = userDto.getPartner() ;
                    }
                } catch (Exception e) {
                    throw new PartnerNotFoundException("Partner Not found");
                }

            }
        }


        return  partnerId ;
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
                                    UserQueryDto query = new UserQueryDto();
                                    query.setPartnerId(partnerDto.getId());
                                    for (UserDto user : userService.getAll(query)) {
                                        List<String> userRoles = userService.getRoles(user.getUsername());
                                        if (!userRoles.isEmpty()) {

                                            for (String userRole : userRoles) {
                                                if (userRole.equals(RoleType.SUPPLIER)) {
                                                    userDto = user;
                                                    break;
                                                }

                                            }
                                            break;
                                        }

                                    }
                                    // userDto = userService.getUserByID(partnerDto.getId());
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
                                                        ArrayList<AddressDto> addressDtos = partnerService.getAddresses(partnerDto.getId());
                                                        if (!addressDtos.isEmpty()) {
                                                            supplier.setAddressDto(addressDtos);
                                                        }
                                                        ArrayList<ContactDto> contacts = partnerService.getContacts(partnerDto.getId());
                                                        if (!contacts.isEmpty()) {
                                                            supplier.setContactDto(contacts);
                                                        }
                                                        ArrayList<IdentityDto> Identities = partnerService.getIdentities(partnerDto.getId());

                                                        if (!Identities.isEmpty()) {
                                                            supplier.setIdentityDto(Identities);
                                                        }
                                                        return supplier;
                                                    })
                                                    .orElse(null);
                                        })
                                        .orElse(null);
                            })
                            .orElse(null);
                })
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found"));


        return supplierDto;
    }

    @Override
    public List<SupplierDto> getAll() {
        boolean userSupplier = false;

        List<SupplierDto> supplierDtoList = new ArrayList<>();
        PartnerQueryDto query = new PartnerQueryDto();
        query.setRole(RoleType.SUPPLIER);
        UserDto userDto = new UserDto();
        ArrayList<PartnerDto> supplies = partnerService.search(query);
        if (!supplies.isEmpty()) {
            for (PartnerDto partner : supplies) {
                SupplierDto supplierDto = new SupplierDto();
                UserQueryDto queryUser = new UserQueryDto();
                queryUser.setPartnerId(partner.getId());
                for (UserDto user : userService.getAll(queryUser)) {

                    List<String> userRoles = userService.getRoles(user.getUsername());

                    if (!userRoles.isEmpty()) {
                        for (String userRole : userRoles) {
                            if (userRole.equals(RoleType.SUPPLIER)) {

                                supplierDto.setUsername(user.getUsername());
                                supplierDto.setSupplierType(partner.getType());
                                supplierDto.setPartnerId(partner.getId());
                                if (partner.getType().equals(PartnerType.ORGANIZATION)) {
                                    supplierDto.setOrganizationName(partner.getName1());
                                } else {

                                    supplierDto.setFirstName(partner.getName2());
                                    supplierDto.setMiddleName(partner.getName3());
                                    supplierDto.setLastName(partner.getName1());

                                }
                                ArrayList<AddressDto> addressDtos = partnerService.getAddresses(partner.getId());
                                if (!addressDtos.isEmpty()) {
                                    supplierDto.setAddressDto(addressDtos);
                                }
                                ArrayList<ContactDto> contacts = partnerService.getContacts(partner.getId());
                                if (!contacts.isEmpty()) {
                                    supplierDto.setContactDto(contacts);
                                }

                                ArrayList<IdentityDto> Identities = partnerService.getIdentities(partner.getId());

                                if (!Identities.isEmpty()) {
                                    supplierDto.setIdentityDto(Identities);
                                }
                                supplierDtoList.add(supplierDto);
                                break;
                            }

                        }
                        break;
                    }

                }


//

            }
        }
        return supplierDtoList;
    }


}
