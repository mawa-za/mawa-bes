package za.co.mawa.bes.billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BillingEntitlementClientTest {

    @Test
    void idTokenFailureUsesExistingFirstLookupFallbackInsteadOfBreakingMembershipRequests() {
        CloudRunIdTokenProvider tokens = mock(CloudRunIdTokenProvider.class);
        doThrow(new IllegalStateException(
                "Unable to obtain a Cloud Run identity token for billing",
                new IOException("metadata server unavailable")))
                .when(tokens).apply(any(HttpHeaders.class));

        BillingEntitlementClient client = new BillingEntitlementClient(
                new RestTemplateBuilder(), tokens, "https://billing.example.run.app",
                "", 30, 200, 300);

        // An uncached entitlement lookup already fails open on network errors.
        // ID-token failures must follow the same established policy.
        assertTrue(client.isEnabled("test-tenant", "MEMBERSHIP"));
        verify(tokens).apply(any(HttpHeaders.class));
    }
}
