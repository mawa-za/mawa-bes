package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.entity.TenantPropertyEntity;
import za.co.raretag.mawabes.object.tenant.TenantDAO;
import za.co.raretag.mawabes.entity.TenantEntity;
import za.co.raretag.mawabes.repository.TenantPropertyRepository;
import za.co.raretag.mawabes.repository.TenantRepository;

import java.util.List;
import java.util.Properties;

@Component
public class TenantService implements TenantDAO {

    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    TenantPropertyRepository tenantPropertyRepository;
    @Override
    public List<TenantEntity> getAll() {
        return tenantRepository.findAll();
    }

    @Override
    public Properties getTenantProperties(String tenant) {
        Properties properties = new Properties();
        List<TenantPropertyEntity>  propertyEntities = tenantPropertyRepository.findAll();
        for(TenantPropertyEntity tenantPropertyEntity: propertyEntities){
            if (tenantPropertyEntity.getTenant().equals(tenant)){
                properties.put(tenantPropertyEntity.getProperty(), tenantPropertyEntity.getValue());
            }
        }
        return properties;
    }
}
