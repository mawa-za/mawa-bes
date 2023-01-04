package za.co.raretag.mawabes.object.tenant;

import za.co.raretag.mawabes.entity.TenantEntity;
import za.co.raretag.mawabes.entity.TenantPropertyEntity;

import java.util.List;
import java.util.Properties;

public interface TenantDAO {
    List<TenantEntity> getAll();
    Properties getTenantProperties(String tenant);
}
