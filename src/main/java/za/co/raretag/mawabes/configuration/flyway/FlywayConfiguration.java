package za.co.raretag.mawabes.configuration.flyway;

import org.apache.commons.dbcp.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Environment;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Properties;

import static za.co.raretag.mawabes.configuration.hibernate.SchemaMultiTenantConnectionProvider.HIBERNATE_PROPERTIES_PATH;


@Component
public class FlywayConfiguration {

//    @Autowired
//    TenantService tenantService;
    private static final String DB_MIGRATION_TENANTS = "db/migration/all";
    private static final String DB_MIGRATION_SPECIFIC_FOR_TENANT = "db/migration/%s";

    @PostConstruct
    Boolean tenantSchemaFlyway() {
//        migrateTenants("default");
        return true;
    }

    private void migrateTenants(String tenantId) {
        Pair<String, BasicDataSource> data = dataSource(tenantId);
        Flyway.configure()
                .locations(DB_MIGRATION_TENANTS, String.format(DB_MIGRATION_SPECIFIC_FOR_TENANT, tenantId))
                .baselineOnMigrate(true)
                .dataSource(data.getSecond())
                .schemas(data.getFirst())
                .load()
                .migrate();
    }

    private void updateTenants(String tenantId) {
        Pair<String, BasicDataSource> data = dataSource(tenantId);
        Flyway.configure()
                .locations(DB_MIGRATION_TENANTS, String.format(DB_MIGRATION_SPECIFIC_FOR_TENANT, tenantId))
                .baselineOnMigrate(true)
                .dataSource(data.getSecond())
                .schemas(data.getFirst())
                .load()
                .migrate();
    }

    public Pair<String, BasicDataSource> dataSource(String tenantId) {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream(String.format(HIBERNATE_PROPERTIES_PATH, tenantId)));
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(properties.get(Environment.DRIVER).toString());
            dataSource.setUrl(properties.get(Environment.URL).toString());
            dataSource.setUsername(properties.get(Environment.USER).toString());
            dataSource.setPassword(properties.get(Environment.PASS).toString());
            return Pair.of(properties.get(Environment.DEFAULT_SCHEMA).toString(),  dataSource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}