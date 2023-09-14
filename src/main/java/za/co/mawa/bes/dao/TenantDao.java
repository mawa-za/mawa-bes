package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.TenantPropertyDto;

import java.util.List;
import java.util.Properties;

public interface TenantDao {

    TenantDto create(TenantDto tenantDto) throws Exception;
    List<TenantDto> getAll();
    void addProperty(TenantPropertyDto tenantPropertyDto);

    Properties getTenantProperties(String tenant);
}
