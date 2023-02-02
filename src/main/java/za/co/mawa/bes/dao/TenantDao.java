package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.TenantDto;

import java.util.List;
import java.util.Properties;

public interface TenantDao {
    String getAdminToken();
    List<TenantDto> getAll();
    Properties getTenantProperties(String tenant);
}
