package za.co.mawa.bes.xero;

import org.junit.jupiter.api.Test;
import java.net.HttpURLConnection;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XeroAccountingEndpointTest {
    @Test void resolvesRootAndLegacyFullUrlsForAllResources() {
        for (String resource : new String[]{"Invoices", "Payments", "CreditNotes", "Contacts", "Items", "Accounts", "TaxRates"}) {
            String expected = XeroAccountingEndpoint.ROOT + "/" + resource;
            assertEquals(expected, XeroAccountingEndpoint.resolve(null, resource));
            assertEquals(expected, XeroAccountingEndpoint.resolve(XeroAccountingEndpoint.ROOT + "/", resource));
            assertEquals(expected, XeroAccountingEndpoint.resolve(expected + "/", resource));
        }
    }
    @Test void rejectsWrongResourceAndExternalHost() {
        assertThrows(IllegalArgumentException.class, () -> XeroAccountingEndpoint.resolve(XeroAccountingEndpoint.ROOT + "/Payments", "Invoices"));
        assertThrows(IllegalArgumentException.class, () -> XeroAccountingEndpoint.resolve("https://example.org/api.xro/2.0", "Invoices"));
    }
    @Test void empty404DoesNotReadSuccessStream() throws Exception {
        var connection = mock(HttpURLConnection.class);
        assertEquals("", XeroAccountingEndpoint.readBody(connection, 404));
        verify(connection, never()).getInputStream();
    }
    @Test void preservesErrorAndSuccessBodies() throws Exception {
        var connection = mock(HttpURLConnection.class);
        when(connection.getErrorStream()).thenReturn(new ByteArrayInputStream("invalid".getBytes()));
        assertEquals("invalid", XeroAccountingEndpoint.readBody(connection, 400));
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
        assertEquals("{}", XeroAccountingEndpoint.readBody(connection, 200));
    }
}
