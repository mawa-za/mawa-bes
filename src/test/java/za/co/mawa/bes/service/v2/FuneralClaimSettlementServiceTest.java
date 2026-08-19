package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FuneralClaimSettlementServiceTest {

    @Test
    void settlementUsesFuneralProviderInvoiceAllocationAsPaymentAmount() {
        long amount = FuneralClaimSettlementService.settlementAmountCents(Map.of(
                "amount_cents", 18_500L,
                "invoice_no", "INV-000123"
        ));

        assertEquals(18_500L, amount);
    }

    @Test
    void externalOnlyInvoiceUsesPartnerMappedInFuneralTenantIntegration() {
        String partnerId = FuneralClaimSettlementService.configuredExternalInvoicePartnerId(
                Map.of(
                        "membership_source_mode", "EXTERNAL_ONLY",
                        "external_tenant_id", "tenant-b",
                        "external_tenant_partner_id", "tenant-b-debtor-in-tenant-a",
                        "active", true
                ),
                "tenant-b"
        );

        assertEquals("tenant-b-debtor-in-tenant-a", partnerId);
    }

    @Test
    void localOnlyIntegrationDoesNotOverrideClaimInvoicePartner() {
        String partnerId = FuneralClaimSettlementService.configuredExternalInvoicePartnerId(
                Map.of(
                        "membership_source_mode", "LOCAL_ONLY",
                        "external_tenant_id", "tenant-b",
                        "external_tenant_partner_id", "partner-a",
                        "active", true
                ),
                "tenant-b"
        );

        assertNull(partnerId);
    }

    @Test
    void repairedCombinationInvoiceCannotExceedRemainingFuneralBalance() {
        long amount = FuneralClaimSettlementService.capCoverageInvoiceAmount(
                20_000L,
                30_000L,
                20_000L
        );
        assertEquals(10_000L, amount);
    }
}
