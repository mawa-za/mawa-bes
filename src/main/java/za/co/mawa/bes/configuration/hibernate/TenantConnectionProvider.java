package za.co.mawa.bes.configuration.hibernate;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

// @Component
@RequiredArgsConstructor
public class TenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {

    @Autowired
    private final DataSource dataSource;

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSource;
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return dataSource;
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.createStatement().execute(String.format("USE %s;", tenantIdentifier));
        return connection;
    }
}
