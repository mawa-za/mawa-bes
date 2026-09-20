package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.repository.PartnerContactRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XeroMasterDataPushServiceTest {

    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerContactRepository contactRepository;
    @Mock private ProductRepository productRepository;
    @Mock private XeroAuthService authService;
    @Mock private XeroIntegrationSettingsService settings;

    @Test
    void productPayloadIsConfiguredForSalesInvoiceLines() {
        when(settings.invoiceAccountCode()).thenReturn("200");
        when(settings.invoiceTaxType()).thenReturn("NONE");
        XeroMasterDataPushService service = new XeroMasterDataPushService(
                partnerRepository, contactRepository, productRepository, authService, settings);
        ProductEntity product = ProductEntity.builder()
                .id("product-1")
                .code("PLAN-1")
                .description("Funeral plan")
                .availableForSale(false)
                .build();

        JsonNode item = service.buildProductPayload(product).path("Items").get(0);

        assertEquals("PLAN-1", item.path("Code").asText());
        assertTrue(item.path("IsSold").asBoolean());
        assertEquals("200", item.path("SalesDetails").path("AccountCode").asText());
        assertEquals("NONE", item.path("SalesDetails").path("TaxType").asText());
    }
}
