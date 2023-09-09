package za.co.mawa.bes.configuration.hibernate;

import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.RemoteTenantService;
import za.co.mawa.bes.service.TenantService;

import java.io.IOException;
import java.util.*;
//@Component
public class DatabaseMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider {
    @Autowired
    TenantService tenantService;
    public static final String HIBERNATE_PROPERTIES_PATH = "/application-%s.properties";
    private final Map<String, ConnectionProvider> connectionProviderMap;

    public DatabaseMultiTenantConnectionProvider() {
        this.connectionProviderMap = new HashMap<String, ConnectionProvider>();
    }

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return getConnectionProvider(TenantContext.DEFAULT_TENANT_ID);
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        return getConnectionProvider(tenantIdentifier);
    }

    private ConnectionProvider getConnectionProvider(String tenantIdentifier) {
        return Optional.ofNullable(tenantIdentifier)
                .map(connectionProviderMap::get)
                .orElseGet(() -> createNewConnectionProvider(tenantIdentifier));
    }

    private ConnectionProvider createNewConnectionProvider(String tenantIdentifier) {
        return Optional.ofNullable(tenantIdentifier)
                .map(this::createConnectionProvider)
                .map(connectionProvider -> {
                    connectionProviderMap.put(tenantIdentifier, connectionProvider);
                    return connectionProvider;
                })
                .orElseThrow(() -> new ConnectionProviderException("Cannot create new connection provider for tenant: " + tenantIdentifier));
    }

    private ConnectionProvider createConnectionProvider(String tenantIdentifier) {
        return Optional.ofNullable(tenantIdentifier)
                .map(this::getHibernatePropertiesForTenantId)
                .map(this::initConnectionProvider)
                .orElse(null);
    }

    private Properties getHibernatePropertiesForTenantId(String tenantId) {
        try {
            return tenantService.getTenantProperties(tenantId);
        } catch (NullPointerException ex) {
            try{
                Properties properties = new Properties();
                String path = String.format(HIBERNATE_PROPERTIES_PATH, tenantId);
                properties.load(getClass().getResourceAsStream(String.format(HIBERNATE_PROPERTIES_PATH, tenantId)));
                return properties;
            }catch (IOException e){
                throw new RuntimeException("Cannot open hibernate properties: " + String.format(HIBERNATE_PROPERTIES_PATH, tenantId));
            }

        }
    }

    private ConnectionProvider initConnectionProvider(Properties hibernateProperties) {
        DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl();
        Map props = hibernateProperties;
        connectionProvider.configure(props);
        return connectionProvider;
    }

}
