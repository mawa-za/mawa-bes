package za.co.mawa.bes.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QueuedOutgoingEmailServiceTest {
    @AfterEach void clean() { TenantContext.clear(); }

    @Test void queuePersistsSerializedMimeWithoutCallingSmtp() {
        TenantContext.setCurrentTenant("qa_tenant");
        JavaMailSender mail = mock(JavaMailSender.class);
        when(mail.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        CompanyInfoService info = mock(CompanyInfoService.class);
        when(info.getCompanyName()).thenReturn("QA Tenant");
        when(info.getCompanyEmail()).thenReturn("finance@example.com");
        SharedOutgoingEmailService transport = new SharedOutgoingEmailService(
                mail, info, "noreply@mawa.co.za", "MAWA");
        TenantOutgoingEmailOutboxStore store = mock(TenantOutgoingEmailOutboxStore.class);
        when(store.enqueue(eq("invoice"), eq("invoice1"), anyString(), any(byte[].class))).thenReturn("queued-1");
        QueuedOutgoingEmailService service = new QueuedOutgoingEmailService(transport, store);
        assertEquals("queued-1", service.queue("buyer@example.com", "Invoice A", "<p>Hi</p>",
                List.of(), "invoice", "invoice1"));
        verify(store).enqueue(eq("invoice"), eq("invoice1"),
                argThat(key -> key.matches("[a-f0-9]{64}")),
                argThat(payload -> payload.length > 100));
        verify(mail, never()).send(any(MimeMessage.class));
    }

    @Test void sha256FingerprintIsStable() {
        assertEquals(QueuedOutgoingEmailService.hash("a"), QueuedOutgoingEmailService.hash("a"));
        assertNotEquals(QueuedOutgoingEmailService.hash("a"), QueuedOutgoingEmailService.hash("b"));
    }
}
