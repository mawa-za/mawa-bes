package za.co.mawa.bes.xero;

import org.junit.jupiter.api.Test;
import za.co.mawa.bes.service.SettingService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XeroOAuthStateServiceTest {
    private final SettingService settings = mock(SettingService.class);
    private final Map<String, String> values = new HashMap<>();

    private XeroOAuthStateService service(Clock clock) {
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(2));
            return null;
        }).when(settings).upsertSetting(anyString(), eq("XERO"), anyString());
        when(settings.getSetting(anyString(), eq("XERO"))).thenAnswer(invocation ->
                values.get(invocation.getArgument(0)));
        return new XeroOAuthStateService(settings, new java.security.SecureRandom(), clock);
    }

    @Test void validCallbackStateIsAcceptedOnceOnly() {
        XeroOAuthStateService stateService = service(Clock.systemUTC());
        String state = stateService.issue("phanka.app.mawa.co.za");
        assertEquals("phanka.app.mawa.co.za", XeroOAuthStateService.tenantHostHint(state));
        assertFalse(values.get("OAUTH-STATE-HASH").contains(state));
        stateService.validateAndConsume(state, "phanka.app.mawa.co.za");
        assertThrows(IllegalArgumentException.class,
                () -> stateService.validateAndConsume(state, "phanka.app.mawa.co.za"));
    }

    @Test void callbackForDifferentHostCannotConsumeAnotherTenantsState() {
        XeroOAuthStateService stateService = service(Clock.systemUTC());
        String state = stateService.issue("phanka.app.mawa.co.za");
        assertThrows(IllegalArgumentException.class,
                () -> stateService.validateAndConsume(state, "other.app.mawa.co.za"));
    }

    @Test void stateExpiresAfterTenMinutes() {
        Clock issuedAt = Clock.fixed(Instant.parse("2026-11-24T18:00:00Z"), ZoneOffset.UTC);
        String state = service(issuedAt).issue("phanka.app.mawa.co.za");
        XeroOAuthStateService later = service(Clock.fixed(Instant.parse("2026-11-24T18:11:00Z"), ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class,
                () -> later.validateAndConsume(state, "phanka.app.mawa.co.za"));
    }

    @Test void oldPredictableStateIsRejected() {
        assertNull(XeroOAuthStateService.tenantHostHint("phanka.app.mawa.co.za"));
        assertNull(XeroOAuthStateService.tenantHostHint(""));
        assertNull(XeroOAuthStateService.tenantHostHint(null));
    }

    @Test void newlyIssuedStateInvalidatesPriorState() {
        XeroOAuthStateService stateService = service(Clock.systemUTC());
        String first = stateService.issue("phanka.app.mawa.co.za");
        String second = stateService.issue("phanka.app.mawa.co.za");
        assertNotEquals(first, second);
        assertThrows(IllegalArgumentException.class,
                () -> stateService.validateAndConsume(first, "phanka.app.mawa.co.za"));
        assertDoesNotThrow(() -> stateService.validateAndConsume(second, "phanka.app.mawa.co.za"));
    }
}
