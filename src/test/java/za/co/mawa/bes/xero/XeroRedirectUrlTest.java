package za.co.mawa.bes.xero;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XeroRedirectUrlTest {
    private static final String CALLBACK = "https://hotfix.api.app.mawa.co.za/xero/callback";

    @Test void backendBaseUrlBecomesCallback() {
        assertEquals(CALLBACK, XeroAuthService.normalizeXeroRedirectUrl("https://hotfix.api.app.mawa.co.za"));
    }

    @Test void existingCallbackRemainsUnchanged() {
        assertEquals(CALLBACK, XeroAuthService.normalizeXeroRedirectUrl(CALLBACK));
    }

    @Test void trailingSlashIsIgnored() {
        assertEquals(CALLBACK, XeroAuthService.normalizeXeroRedirectUrl(CALLBACK + "/"));
    }

    @Test void legacyDoubleCallbackIsRepaired() {
        assertEquals(CALLBACK, XeroAuthService.normalizeXeroRedirectUrl(CALLBACK + "/xero/callback"));
    }

    @Test void nonHttpsRedirectIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> XeroAuthService.normalizeXeroRedirectUrl("http://hotfix.api.app.mawa.co.za"));
    }
}
