package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.OrganizationDto;
import za.co.mawa.bes.dto.PartnerQueryDto;

public interface OrganizationDao {
    String addOrganization (OrganizationDto organization);
    PartnerQueryDto getOrganization();
}
