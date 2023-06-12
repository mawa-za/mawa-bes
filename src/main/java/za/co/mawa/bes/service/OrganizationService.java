package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.OrganizationDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.exception.PartnerNotFound;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationService implements OrganizationDao {
    @Autowired
    UserService userService;
    @Autowired
    PartnerService partnerService;

    @Override
    public String addOrganization(OrganizationDto organization) {
        PartnerDto partner = new PartnerDto();
        partner.setType(PartnerType.ORGANIZATION);
        partner.setName1(organization.getName1());
        partner.setCreatedBy(userService.getCurrentUser());
        String id = partnerService.create(partner).getId();
        if (organization.getBusinessAddress() != null) {
            organization.getBusinessAddress().setPartner(id);
            organization.getBusinessAddress().setType(AddressType.BUSINESS);
            partnerService.addAddress(organization.getBusinessAddress());
        }
        if (organization.getRegNumber() != null) {
            IdentityDto identityDto = new IdentityDto();
            identityDto.setPartner(id);
            identityDto.setIdType(IdType.REG_NUMBER);
            identityDto.setIdNumber(organization.getRegNumber());
            partnerService.addIdentity(identityDto);

        }
        if (organization.getEmailAddress() != null) {
            ContactDto contactDto = new ContactDto();
            contactDto.setPartner(id);
            contactDto.setValue(organization.getEmailAddress());
            contactDto.setType(ContactType.EMAIL);
            partnerService.addContact(contactDto);
        }

        if (organization.getTelephoneNumber() != null) {
            ContactDto contactDto = new ContactDto();
            contactDto.setPartner(id);
            contactDto.setValue(organization.getTelephoneNumber());
            contactDto.setType(ContactType.TELEPHONE);
            partnerService.addContact(contactDto);
        }

        if (organization.getVATNo() != null) {
            IdentityDto identityDto = new IdentityDto();
            identityDto.setPartner(id);
            identityDto.setIdType(IdType.VAT_NUMBER);
            identityDto.setIdNumber(organization.getVATNo());
            partnerService.addIdentity(identityDto);
        }
        if (organization.getPostalAddress() != null) {
            organization.getPostalAddress().setPartner(id);
            organization.getPostalAddress().setType(AddressType.POSTAL);
            partnerService.addAddress(organization.getPostalAddress());
        }
        return id;
    }

    @Override
    public PartnerQueryDto getOrganization() {
        PartnerQueryDto query;
        try {
            query = new PartnerQueryDto();
            query.setRole(RoleType.ORGANIZATION);
            partnerService.search(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return query;
    }

    @Override
    public List<PartnerDto> getOrganizations() {
        PartnerQueryDto partnerQueryDto = new PartnerQueryDto();
        partnerQueryDto.setType(PartnerType.ORGANIZATION);
        List<PartnerDto> partnerDtoArrayList = partnerService.search(partnerQueryDto);
//        Sort sort = Sort.by("id").descending();
//        List<PartnerEntity> partners = partnerRepository.findAll(findByCriteria(searchDto), sort);

        return partnerDtoArrayList;
    }

    @Override
    public PartnerDto getSpecOrganization(String id) {

        try {
            PartnerDto partnerDto = partnerService.get(id);
            if (partnerDto.getType().equals(PartnerType.ORGANIZATION)) {
                return partnerDto;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }


}
