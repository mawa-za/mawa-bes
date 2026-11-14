package za.co.mawa.bes.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import za.co.mawa.bes.configuration.context.TenantContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SharedOutgoingEmailServiceTest {
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void rejectsMissingTenantAndInvalidRecipientBeforeTransport() {
        JavaMailSender mail = mock(JavaMailSender.class);
        CompanyInfoService company = mock(CompanyInfoService.class);
        SharedOutgoingEmailService sender = new SharedOutgoingEmailService(mail, company, "noreply@mawa.co.za", "MAWA");
        assertThrows(IllegalStateException.class,
                () -> sender.submit("a@example.com", "Hello", "<p>Hello</p>", null, "test"));
        TenantContext.setCurrentTenant("qa_tenant");
        assertThrows(IllegalArgumentException.class,
                () -> sender.submit("bad-address", "Hello", "<p>Hello</p>", null, "test"));
        verifyNoInteractions(mail);
    }

    @Test void validatesMultipleRecipientsAndPreventsHeaderInjection() {
        assertArrayEquals(new String[]{"a@example.com", "b@example.com"},
                SharedOutgoingEmailService.validateRecipients(" a@example.com ; b@example.com "));
        assertThrows(IllegalArgumentException.class,
                () -> SharedOutgoingEmailService.validatedSingleAddress("a@example.com\nBcc:bad@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> SharedOutgoingEmailService.validateRecipients("a@example.com;"));
    }

    @Test void sendsOnlyCentralFromWithTenantBrandingAndReplyTo() throws Exception {
        TenantContext.setCurrentTenant("qa_tenant");
        JavaMailSender mail = mock(JavaMailSender.class);
        MimeMessage mime = new MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()));
        when(mail.createMimeMessage()).thenReturn(mime);
        CompanyInfoService company = mock(CompanyInfoService.class);
        when(company.getCompanyName()).thenReturn("QA Society");
        when(company.getCompanyEmail()).thenReturn("accounts@example.com");
        SharedOutgoingEmailService sender = new SharedOutgoingEmailService(mail, company, "noreply@mawa.co.za", "MAWA");
        String requestId = sender.submit("test@example.com", "Invoice", "<p>Test</p>", null, "invoice");
        assertNotNull(requestId);
        verify(mail).send(any(MimeMessage.class));
        assertEquals("noreply@mawa.co.za", ((jakarta.mail.internet.InternetAddress) mime.getFrom()[0]).getAddress());
        assertEquals("accounts@example.com", ((jakarta.mail.internet.InternetAddress) mime.getReplyTo()[0]).getAddress());
    }
}
