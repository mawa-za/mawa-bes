package za.co.raretag.mawabes.configuration.hibernate;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import za.co.raretag.mawabes.configuration.context.TenantContext;

import java.util.Optional;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return Optional.ofNullable(TenantContext.getCurrentTenant())
                .orElse(TenantContext.DEFAULT_TENANT_ID);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

}