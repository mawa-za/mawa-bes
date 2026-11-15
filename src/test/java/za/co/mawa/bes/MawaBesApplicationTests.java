package za.co.mawa.bes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full application-context integration test. Requires a dedicated disposable
 * MySQL database with MAWA tenant baseline migrations already applied.
 *
 * Without a test database, the test is SKIPPED (not treated as proof that the
 * application context has started). Never point these variables at prod.
 */
@EnabledIfEnvironmentVariable(named = "MAWA_TEST_JDBC_URL", matches = "jdbc:mysql:.*")
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=${MAWA_TEST_JDBC_URL}",
        "spring.datasource.username=${MAWA_TEST_DB_USERNAME:}",
        "spring.datasource.password=${MAWA_TEST_DB_PASSWORD:}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.generate-ddl=false",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "gcp.secret-manager.enabled=false"
})
class MawaBesApplicationTests {

    @Test
    void contextLoads() {
    }
}
