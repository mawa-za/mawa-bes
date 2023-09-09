package za.co.mawa.bes.configuration.hibernate;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.util.Map;
import java.util.Optional;

@Component
//@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {
    private String currentTenant = "unknown";

    public void setCurrentTenant(String tenant) {
        currentTenant = tenant;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        return Optional.ofNullable(TenantContext.getCurrentTenant())
                .orElse(TenantContext.DEFAULT_TENANT_ID);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}