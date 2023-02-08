package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.OrganizationDto;

public interface OrganizationDao {
    String addOrganization (OrganizationDto organization);
    String getOrganization();
}
