package za.co.raretag.mawabes.model;

import java.util.Properties;

public interface TenantDao {
    String getAdminToken();
//    List<TenantEntity> getAll();
    Properties getTenantProperties(String tenant);
}
