package za.co.mawa.bes.configuration.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TenantHostNormalizerTest {

    @Test
    void normalizesFlutterWebUrlContainingHashRoute() {
        assertEquals(
                "dev.app.mawa.co.za",
                TenantHostNormalizer.normalize("dev.app.mawa.co.za/#/login")
        );
    }

    @Test
    void normalizesAbsoluteUrlContainingHashRoute() {
        assertEquals(
                "dev.app.mawa.co.za",
                TenantHostNormalizer.normalize("https://dev.app.mawa.co.za/#/login")
        );
    }

    @Test
    void normalizesOriginWithPortAndMixedCase() {
        assertEquals(
                "dev.app.mawa.co.za",
                TenantHostNormalizer.normalize("HTTPS://DEV.APP.MAWA.CO.ZA:443")
        );
    }

    @Test
    void usesFirstForwardedHost() {
        assertEquals(
                "dev.app.mawa.co.za",
                TenantHostNormalizer.normalize("dev.app.mawa.co.za, proxy.internal")
        );
    }

    @Test
    void returnsNullForBlankValues() {
        assertNull(TenantHostNormalizer.normalize("  "));
    }
}
