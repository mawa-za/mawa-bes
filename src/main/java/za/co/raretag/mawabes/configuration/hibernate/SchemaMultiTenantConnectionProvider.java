package za.co.raretag.mawabes.configuration.hibernate;

import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import za.co.raretag.mawabes.configuration.context.TenantContext;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@SuppressWarnings("serial")
public class SchemaMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider {
    //public static final String HIBERNATE_PROPERTIES_PATH = "/application.properties";

    public static final String HIBERNATE_PROPERTIES_PATH = "/hibernate-%s.properties";
    private final Map<String, ConnectionProvider> connectionProviderMap;

    public SchemaMultiTenantConnectionProvider() {
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
                .orElseThrow(() -> new ConnectionProviderException("Cannot create new connection provider for tenant: "+tenantIdentifier));
    }

    private ConnectionProvider createConnectionProvider(String tenantIdentifier) {
        return Optional.ofNullable(tenantIdentifier)
                .map(this::getHibernatePropertiesForTenantId)
                .map(this::initConnectionProvider)
                .orElse(null);
    }

    private Properties getHibernatePropertiesForTenantId(String tenantId) {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream(String.format(HIBERNATE_PROPERTIES_PATH, tenantId)));
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("Cannot open hibernate properties: "+ HIBERNATE_PROPERTIES_PATH);
        }
    }

    private ConnectionProvider initConnectionProvider(Properties hibernateProperties) {
        DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl();
        Map props = hibernateProperties;
        connectionProvider.configure(props);
        return connectionProvider;
    }

}
