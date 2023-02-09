package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.OrganizationDto;
import za.co.raretag.mawabes.dto.PartnerQueryDto;

public interface OrganizationDao {
    String addOrganization (OrganizationDto organization);
    PartnerQueryDto getOrganization();
}
