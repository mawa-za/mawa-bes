package za.co.mawa.bes.dao;

import java.util.Properties;

public interface TenantDao {
    String getAdminToken();
//    List<TenantEntity> getAll();
    Properties getTenantProperties(String tenant);
}
