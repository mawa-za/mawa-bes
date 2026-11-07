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
}
