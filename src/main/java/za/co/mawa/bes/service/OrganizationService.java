package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.OrganizationDao;
import za.co.mawa.bes.dto.OrganizationDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.utils.AddressType;
import za.co.mawa.bes.utils.PartnerType;
import za.co.mawa.bes.utils.RoleType;
import za.co.raretag.mawabes.dto.PartnerQueryDto;

import java.util.ArrayList;
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
        String id = partnerService.create(partner);
        if (organization.getBusinessAddress() != null) {
            organization.getBusinessAddress().setPartner(id);
            organization.getBusinessAddress().setType(AddressType.BUSINESS);
            partnerService.addAddress(organization.getBusinessAddress());
        }
        if (organization.getPostalAddress() != null) {
            organization.getPostalAddress().setPartner(id);
            organization.getPostalAddress().setType(AddressType.POSTAL);
            partnerService.addAddress(organization.getPostalAddress());
        }
        return id;
    }

    @Override
    public String getOrganization() {
        PartnerQueryDto query = new PartnerQueryDto();
        ArrayList<ArrayList<PartnerDto>> organization = new ArrayList<>();
        query.setRole(RoleType.ORGANIZATION);
        ArrayList<PartnerDto> partners =  partnerService.search(query);
        if(partners != null){
            organization.add(partners);
        }
        return null;
    }
}
