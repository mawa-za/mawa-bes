package za.co.mawa.bes.billing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BillingModuleRouteResolverTest {

    private final BillingModuleRouteResolver resolver = new BillingModuleRouteResolver();

    @Test
    void resolvesCoreModuleRoutes() {
        assertEquals("MEMBERSHIP", resolver.resolve("/v2/group-society/42"));
        assertEquals("MEMBERSHIP", resolver.resolve("/v2/premium-generation/run"));
        assertEquals("CLAIMS", resolver.resolve("/v2/membership-claim/42/approve"));
        assertEquals("FUNERAL", resolver.resolve("/v2/funeral-underwriting/rules"));
        assertEquals("TOMBSTONES", resolver.resolve("/v2/tombstones/orders/42"));
        assertEquals("INVENTORY", resolver.resolve("/purchase-order/42"));
        assertEquals("HR", resolver.resolve("/v2/leave-request/42"));
        assertEquals("HR", resolver.resolve("/v2/payroll-payment-batch/42"));
        assertEquals("HR", resolver.resolve("/v2/assets/42"));
        assertEquals("ACCOUNTING", resolver.resolve("/v2/invoice/42"));
        assertEquals("ACCOUNTING", resolver.resolve("/v2/cashup/42"));
        assertEquals("ACCOUNTING", resolver.resolve("/cashup-range/42"));
        assertEquals("PAYMENTS", resolver.resolve("/v2/payment-request/42"));
        assertEquals("PAYMENTS", resolver.resolve("/v2/pay-app/members"));
        assertEquals("LEGAL", resolver.resolve("/v2/cases/42"));
        assertEquals("CALENDAR", resolver.resolve("/v2/appointment/calendar"));
        assertEquals("DOCUMENTS", resolver.resolve("/v2/attachment/42"));
        assertEquals("POS", resolver.resolve("/v2/pos-print-agents/42"));
    }

    @Test
    void doesNotMatchUnrelatedOrPartialRoutes() {
        assertNull(resolver.resolve("/partner/42"));
        assertNull(resolver.resolve("/invoice-preview/42"));
        assertNull(resolver.resolve(null));
    }
}
