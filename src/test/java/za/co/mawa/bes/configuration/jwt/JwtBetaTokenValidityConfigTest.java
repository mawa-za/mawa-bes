package za.co.mawa.bes.configuration.jwt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class JwtBetaTokenValidityConfigTest {
    @Test
    void betaUsesTheSameLongLivedDefaultsAsProduction() throws IOException {
        Properties properties = new Properties();
        try (var resource = getClass().getResourceAsStream("/application-beta.properties")) {
            assertNotNull(resource, "application-beta.properties must be present");
            properties.load(resource);
        }
        assertEquals("${JWT_ACCESS_EXPIRATION_MS:2592000000}",
                properties.getProperty("jwt.expirationDateInMs"));
        assertEquals("${JWT_REFRESH_EXPIRATION_MS:31536000000}",
                properties.getProperty("jwt.refreshExpirationDateInMs"));
    }
}
