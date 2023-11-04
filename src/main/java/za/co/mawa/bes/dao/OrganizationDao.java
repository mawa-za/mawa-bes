package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.OrganizationDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PartnerQueryDto;

import java.util.List;

public interface OrganizationDao {
    String addOrganization (OrganizationDto organization);
    PartnerQueryDto getOrganization();

    List<PartnerDto> getOrganizations();

    PartnerDto getSpecOrganization(String id) ;




}
