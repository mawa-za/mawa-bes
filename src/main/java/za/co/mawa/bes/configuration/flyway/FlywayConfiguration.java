package za.co.mawa.bes.configuration.flyway;

import jakarta.annotation.PostConstruct;
import org.apache.commons.dbcp.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.service.EncryptionService;
import za.co.mawa.bes.service.TenantService;

import java.util.Properties;

@Component
public class FlywayConfiguration {
    @Autowired
    EncryptionService encryptionService;
    @Autowired
    TenantService tenantService;
    private static final String DB_MIGRATION_TENANTS = "db/migration/all";
    private static final String DB_MIGRATION_SPECIFIC_FOR_TENANT = "db/migration/%s";

    @PostConstruct
    Boolean tenantSchemaFlyway() {
        for (TenantDto tenant : tenantService.getAll()) {
            updateTenantDB(tenant.getId());
        }
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

    private void updateTenantDB(String tenantId) {
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
            Properties properties = tenantService.getTenantProperties(tenantId);
//            properties.load(getClass().getResourceAsStream(String.format(HIBERNATE_PROPERTIES_PATH, tenantId)));
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(properties.get(Environment.DRIVER).toString());
            dataSource.setUrl(properties.get(Environment.URL).toString());
            dataSource.setUsername(properties.get(Environment.USER).toString());
            String password = encryptionService.decrypt(properties.get(Environment.PASS).toString(),properties.get("jwt.secret").toString());
            dataSource.setPassword(password);
            return Pair.of(properties.get(Environment.DEFAULT_SCHEMA).toString(), dataSource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}