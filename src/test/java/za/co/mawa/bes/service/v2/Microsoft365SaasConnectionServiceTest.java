package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Microsoft365SaasConnectionServiceTest {
    @Test void oauthStateRequiresTenantPrefixAndStrongNonce() {
        assertTrue(Microsoft365SaasConnectionService.validStateFormat("tenant_1." + "A".repeat(43)));
        assertFalse(Microsoft365SaasConnectionService.validStateFormat("tenant_1." + "x"));
        assertFalse(Microsoft365SaasConnectionService.validStateFormat("../admin." + "A".repeat(43)));
        assertFalse(Microsoft365SaasConnectionService.validStateFormat("tenant_1." + "A".repeat(43) + "/evil"));
    }

    @Test void stateFingerprintsAreStableAndUnique() {
        String a = Microsoft365SaasConnectionService.sha256("tenant.alpha");
        assertEquals(a, Microsoft365SaasConnectionService.sha256("tenant.alpha"));
        assertNotEquals(a, Microsoft365SaasConnectionService.sha256("tenant.beta"));
        assertEquals(64, a.length());
    }

    @Test void authorizationUrlUsesSupportedConsentPrompt() {
        String state = "tenant_1." + "A".repeat(43);
        String nonce = "B".repeat(43);
        String callback = "https://hotfix.api.app.mawa.co.za/v2/bank-transactions/microsoft/callback";
        String url = Microsoft365SaasConnectionService.buildAuthorizationUrl(
                "8dd6cb73-9821-4a14-a253-1fc4ef7c9bd1", callback, state, nonce);
        assertTrue(url.startsWith("https://login.microsoftonline.com/organizations/oauth2/v2.0/authorize?"));
        assertTrue(url.contains("&response_type=code&response_mode=query"));
        assertTrue(url.contains("&scope=openid+profile"));
        assertTrue(url.contains("&prompt=consent"));
        assertFalse(url.contains("admin_consent"));
        assertTrue(url.contains("&state=" + state));
        assertTrue(url.contains("&nonce=" + nonce));
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fhotfix.api.app.mawa.co.za%2Fv2%2Fbank-transactions%2Fmicrosoft%2Fcallback"));
    }
}
