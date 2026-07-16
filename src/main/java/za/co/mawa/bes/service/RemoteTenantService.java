package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.TenantDao;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.TenantPropertyDto;

import java.util.List;
import java.util.Properties;

/**
 * Compatibility facade for older code. TenantAdminService is the single
 * implementation of the admin-service integration contract.
 */
@Deprecated
@Service
public class RemoteTenantService implements TenantDao {

    private final TenantAdminService delegate;

    public RemoteTenantService(TenantAdminService delegate) {
        this.delegate = delegate;
    }

    public String getAdminToken() {
        return delegate.getAdminToken();
    }

    @Override
    public TenantDto create(TenantDto tenantDto) throws Exception {
        return delegate.create(tenantDto);
    }

    @Override
    public List<TenantDto> getAll() {
        return delegate.getAll();
    }

    @Override
    public void addProperty(TenantPropertyDto tenantPropertyDto) {
        delegate.addProperty(tenantPropertyDto);
    }

    @Override
    public Properties getTenantProperties(String tenant) {
        return delegate.getTenantProperties(tenant);
    }
}
