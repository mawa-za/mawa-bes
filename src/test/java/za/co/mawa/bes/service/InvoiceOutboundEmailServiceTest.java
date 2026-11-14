package za.co.mawa.bes.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.InvoiceEntity;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvoiceOutboundEmailServiceTest {
    @AfterEach void clean() { TenantContext.clear(); UserContext.clear(); }

    @Test void disabledByDefaultDoesNotQueryInvoiceOrSend() {
        InvoiceService invoices = mock(InvoiceService.class);
        SharedOutgoingEmailService sender = mock(SharedOutgoingEmailService.class);
        InvoiceOutboundEmailService service = new InvoiceOutboundEmailService(invoices, sender, mock(UserAccessService.class), false);
        assertThrows(IllegalStateException.class, () -> service.submit("invoice1", "buyer@example.com"));
        verifyNoInteractions(invoices, sender);
    }

    @Test void blocksBackgroundAndNonInvoiceUsers() {
        TenantContext.setCurrentTenant("qa_tenant");
        UserContext.setCurrentUserId("cashier");
        UserContext.setBackgroundSession(true);
        InvoiceService invoices = mock(InvoiceService.class);
        SharedOutgoingEmailService sender = mock(SharedOutgoingEmailService.class);
        UserAccessService access = mock(UserAccessService.class);
        InvoiceOutboundEmailService service = new InvoiceOutboundEmailService(invoices, sender, access, true);
        assertThrows(SecurityException.class, () -> service.submit("invoice1", "buyer@example.com"));
        UserContext.setBackgroundSession(false);
        assertThrows(SecurityException.class, () -> service.submit("invoice1", "buyer@example.com"));
        verifyNoInteractions(invoices, sender);
    }

    @Test void rejectsDraftAndNeverSubmitsPdf() {
        TenantContext.setCurrentTenant("qa_tenant");
        UserContext.setCurrentUserId("cashier");
        UserAccessService access = mock(UserAccessService.class);
        when(access.hasWorkcentreAccess("invoice", null)).thenReturn(true);
        InvoiceService invoices = mock(InvoiceService.class);
        InvoiceEntity draft = new InvoiceEntity();
        draft.setStatus("DRAFT"); draft.setPartnerId("partner1");
        when(invoices.getInvoice("invoice1")).thenReturn(Optional.of(draft));
        SharedOutgoingEmailService sender = mock(SharedOutgoingEmailService.class);
        InvoiceOutboundEmailService service = new InvoiceOutboundEmailService(invoices, sender, access, true);
        assertThrows(IllegalArgumentException.class, () -> service.submit("invoice1", "buyer@example.com"));
        verifyNoInteractions(sender);
    }

    @Test void submitsIssuedInvoiceWithCurrentPdfAndAcceptedNotDeliveredStatus() {
        TenantContext.setCurrentTenant("qa_tenant");
        UserContext.setCurrentUserId("cashier");
        UserAccessService access = mock(UserAccessService.class);
        when(access.hasWorkcentreAccess("invoice", null)).thenReturn(true);
        InvoiceService invoices = mock(InvoiceService.class);
        InvoiceEntity issued = new InvoiceEntity();
        issued.setId("invoice1"); issued.setInvoiceNo("INV-555");
        issued.setPartnerId("partner1"); issued.setStatus("ISSUED");
        when(invoices.getInvoice("invoice1")).thenReturn(Optional.of(issued));
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.writeBytes("%PDF-test".getBytes(StandardCharsets.UTF_8));
        when(invoices.generateInvoicePdf("invoice1")).thenReturn(pdf);
        SharedOutgoingEmailService sender = mock(SharedOutgoingEmailService.class);
        when(sender.submit(eq("buyer@example.com"), eq("Invoice INV-555"), anyString(), anyList(), eq("invoice")))
                .thenReturn("request1");
        InvoiceOutboundEmailService service = new InvoiceOutboundEmailService(invoices, sender, access, true);
        var result = service.submit("invoice1", "buyer@example.com");
        assertEquals("ACCEPTED", result.status());
        assertEquals("request1", result.requestId());
        verify(sender).submit(eq("buyer@example.com"), eq("Invoice INV-555"), anyString(),
                argThat(files -> files.size() == 1 && "pdf".equals(files.get(0).getType())
                        && "%PDF-test".equals(new String(java.util.Base64.getDecoder().decode(files.get(0).getContent()),
                        StandardCharsets.UTF_8))), eq("invoice"));
    }
}
