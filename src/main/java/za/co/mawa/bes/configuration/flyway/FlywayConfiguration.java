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
import za.co.mawa.bes.service.RemoteTenantService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.service.TenantService;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Component
public class FlywayConfiguration {
    @Autowired
    DataSource dataSource;
    @Autowired
    Environment environment;
    @Autowired
    EncryptionService encryptionService;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    TenantService tenantService;
    private static final String DB_MIGRATION_TENANTS = "db/migration/all";
    private static final String DB_MIGRATION_SPECIFIC_FOR_TENANT = "db/migration/%s";
    private static final String DEFAULT_SCHEMA = "mawa";

    public static final String HIBERNATE_PROPERTIES_PATH = "/application-default.properties";

    @PostConstruct
    Boolean tenantSchemaFlyway() {
        updateDefaultDB();
        for (TenantDto tenant : tenantAdminService.getAll()) {
            updateTenantDB(tenant.getId());
        }
        return true;
    }

    private void updateDefaultDB() {
        Flyway.configure()
                .locations(DB_MIGRATION_TENANTS)
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .schemas(DEFAULT_SCHEMA)
                .createSchemas(true)
                .load()
                .migrate();
    }

    private void updateTenantDB(String tenantId) {
        Pair<String, BasicDataSource> data = dataSource(tenantId);
        Flyway.configure()
                .locations(DB_MIGRATION_TENANTS)
                .baselineOnMigrate(true)
                .dataSource(dataSource)
                .schemas(tenantId)
                .createSchemas(true)
                .load()
                .migrate();
    }

    public Pair<String, BasicDataSource> dataSource(String tenantId) {
        try {
            Properties properties = tenantService.getTenantProperties(tenantId);
//            Properties properties = new Properties();
//            properties.load(getClass().getResourceAsStream(String.format(HIBERNATE_PROPERTIES_PATH, tenantId)));
//            properties.load(getClass().getResourceAsStream(HIBERNATE_PROPERTIES_PATH));
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(properties.get(Environment.DRIVER).toString());
            dataSource.setUrl(properties.get(Environment.URL).toString());
            dataSource.setUsername(properties.get(Environment.USER).toString());
            String password = encryptionService.decrypt(properties.get(Environment.PASS).toString(), properties.get("jwt.secret").toString());
            dataSource.setPassword(password);
            return Pair.of(properties.get(Environment.DEFAULT_SCHEMA).toString(), dataSource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}