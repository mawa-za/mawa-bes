package za.co.mawa.bes.configuration.hibernate;

import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.TenantService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

// @Component
public class DatabaseMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider<String> {

    @Autowired
    TenantService tenantService;

    public static final String HIBERNATE_PROPERTIES_PATH = "/application-%s.properties";

    private final Map<String, ConnectionProvider> connectionProviderMap = new HashMap<>();

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return getConnectionProvider(TenantContext.DEFAULT_TENANT_ID);
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        return getConnectionProvider(tenantIdentifier);
    }

    private ConnectionProvider getConnectionProvider(String tenantIdentifier) {
        String resolvedTenant = tenantIdentifier == null ? TenantContext.DEFAULT_TENANT_ID : tenantIdentifier;
        return connectionProviderMap.computeIfAbsent(resolvedTenant, this::createConnectionProvider);
    }

    private ConnectionProvider createConnectionProvider(String tenantIdentifier) {
        Properties hibernateProperties = getHibernatePropertiesForTenantId(tenantIdentifier);
        return initConnectionProvider(hibernateProperties);
    }

    private Properties getHibernatePropertiesForTenantId(String tenantId) {
        if (tenantService != null) {
            try {
                return tenantService.getTenantProperties(tenantId);
            } catch (RuntimeException ignored) {
                // Fall back to classpath tenant properties below.
            }
        }

        String path = String.format(HIBERNATE_PROPERTIES_PATH, tenantId);
        try {
            Properties properties = new Properties();
            try (var inputStream = getClass().getResourceAsStream(path)) {
                if (inputStream == null) {
                    throw new RuntimeException("Cannot find hibernate properties: " + path);
                }
                properties.load(inputStream);
            }
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("Cannot open hibernate properties: " + path, e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ConnectionProvider initConnectionProvider(Properties hibernateProperties) {
        DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl();
        connectionProvider.configure((Map) hibernateProperties);
        return connectionProvider;
    }
}
