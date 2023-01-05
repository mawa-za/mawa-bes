package za.co.raretag.mawabes.dao;

import java.util.Properties;

public interface TenantDao {
    String getAdminToken();
//    List<TenantEntity> getAll();
    Properties getTenantProperties(String tenant);
}
