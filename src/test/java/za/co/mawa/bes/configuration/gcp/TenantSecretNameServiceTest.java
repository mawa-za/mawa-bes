package za.co.mawa.bes.configuration.gcp;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantSecretNameServiceTest {

    @Test
    void buildsHostBasedTenantSecretNameUsingActiveEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        TenantSecretNameService service = new TenantSecretNameService(environment, null);

        assertEquals(
                "mawa-dev-dev-app-mawa-co-za-xero-client-id",
                service.buildSecretName("https://dev.app.mawa.co.za/#/login", "XERO", "CLIENT-ID")
        );
    }
}
