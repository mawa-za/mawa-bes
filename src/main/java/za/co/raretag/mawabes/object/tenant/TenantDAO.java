package za.co.raretag.mawabes.object.tenant;

import za.co.raretag.mawabes.entity.TenantEntity;
import za.co.raretag.mawabes.entity.TenantPropertyEntity;

import java.util.List;

public interface TenantDAO {
    List<TenantEntity> getAll();
    List<TenantPropertyEntity> getTenantProperties(String tenant);
}
