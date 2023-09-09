package za.co.mawa.bes.configuration.hibernate;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.TenantService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

//@Component
public class DatabaseMultiTenancyConnectionProvider implements MultiTenantConnectionProvider, HibernatePropertiesCustomizer {
    @Autowired
    DataSource dataSource;
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String schema) throws SQLException {

        return dataSource.getConnection();
    }

    @Override
    public void releaseConnection(String s, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> aClass) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> aClass) {
        throw new UnsupportedOperationException("Can't unwrap this.");
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
//        Properties props = tenantService.getTenantProperties(TenantContext.getCurrentTenant());
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}
