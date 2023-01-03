package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.raretag.mawabes.entity.TenantPropertyEntity;
import za.co.raretag.mawabes.object.tenant.TenantDAO;
import za.co.raretag.mawabes.entity.TenantEntity;

import java.util.List;
@Component
public class TenantService implements TenantDAO {
    @Override
    public List<TenantEntity> getAll() {
        return null;
    }

    @Override
    public List<TenantPropertyEntity> getTenantProperties(String tenant) {
        return null;
    }
}
