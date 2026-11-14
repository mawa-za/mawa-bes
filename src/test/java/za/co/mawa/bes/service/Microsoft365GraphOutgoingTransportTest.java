package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Microsoft365GraphOutgoingTransportTest {
    private static final String DIRECTORY = "12345678-1234-1234-1234-123456789012";
    private static final String CLIENT = "abcdefab-abcd-abcd-abcd-abcdefabcdef";
    private static final String MAILBOX = "noreply@mawa.co.za";

    @AfterEach void reset() { TenantContext.clear(); }

    private static byte[] sample(String from) throws Exception {
        MimeMessage mail = new MimeMessage(Session.getInstance(new Properties()));
        mail.setFrom(from);
        mail.setRecipients(jakarta.mail.Message.RecipientType.TO, "recipient@example.com");
        mail.setSubject("Test email");
        mail.setText("MAWA test");
        mail.saveChanges();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mail.writeTo(out);
        return out.toByteArray();
    }

    @Test void graphOnlySendsAsConfiguredMailboxAndReturnsClientMessageId() throws Exception {
        TenantContext.setCurrentTenant("qa_tenant");
        List<HttpRequest> requests = new ArrayList<>();
        Microsoft365GraphOutgoingTransport transport = new Microsoft365GraphOutgoingTransport(
                DIRECTORY, CLIENT, "test-secret", MAILBOX, new ObjectMapper(), request -> {
                    requests.add(request);
                    return new Microsoft365GraphOutgoingTransport.Response(requests.size() == 1 ? 200 : 202,
                            requests.size() == 1 ? "{\"access_token\":\"mock-token\",\"expires_in\":3600}" : "");
                });
        byte[] mime = sample(MAILBOX);
        String id = transport.submitMime(mime, UUID.randomUUID().toString());
        assertNotNull(id);
        assertEquals(2, requests.size());
        assertEquals("login.microsoftonline.com", requests.get(0).uri().getHost());
        assertEquals("graph.microsoft.com", requests.get(1).uri().getHost());
        assertEquals("Bearer mock-token", requests.get(1).headers().firstValue("Authorization").orElse(null));
        assertTrue(requests.get(1).uri().toString().contains("/sendMail"));
        assertEquals("text/plain", requests.get(1).headers().firstValue("Content-Type").orElse(null));
        // OAuth token is cached during the same process.
        transport.submitMime(mime, UUID.randomUUID().toString());
        assertEquals(3, requests.size());
    }

    @Test void wrongFromFailsBeforeCallingMicrosoft() throws Exception {
        TenantContext.setCurrentTenant("qa_tenant");
        Microsoft365GraphOutgoingTransport transport = new Microsoft365GraphOutgoingTransport(
                DIRECTORY, CLIENT, "test-secret", MAILBOX, new ObjectMapper(), request -> {
                    fail("No network calls should happen for forged sender");
                    return null;
                });
        assertThrows(Microsoft365GraphOutgoingTransport.PermanentMailConfigurationException.class,
                () -> transport.submitMime(sample("tenant@example.org"), UUID.randomUUID().toString()));
    }

    @Test void missingTenantAndMissingCredentialFailClosed() throws Exception {
        byte[] mime = sample(MAILBOX);
        Microsoft365GraphOutgoingTransport transport = new Microsoft365GraphOutgoingTransport(
                DIRECTORY, CLIENT, "", MAILBOX, new ObjectMapper(), request -> {
                    fail("No network access expected"); return null;
                });
        assertThrows(IllegalStateException.class,
                () -> transport.submitMime(mime, UUID.randomUUID().toString()));
        TenantContext.setCurrentTenant("qa_tenant");
        assertThrows(Microsoft365GraphOutgoingTransport.PermanentMailConfigurationException.class,
                () -> transport.submitMime(mime, UUID.randomUUID().toString()));
    }

    @Test void graph429IsRetryableAnd403Permanent() throws Exception {
        TenantContext.setCurrentTenant("qa_tenant");
        byte[] mime = sample(MAILBOX);
        for (int status : new int[]{429, 503, 403}) {
            Microsoft365GraphOutgoingTransport transport = new Microsoft365GraphOutgoingTransport(
                    DIRECTORY, CLIENT, "test-secret", MAILBOX, new ObjectMapper(), request -> {
                        return new Microsoft365GraphOutgoingTransport.Response(
                                request.uri().getHost().equals("login.microsoftonline.com") ? 200 : status,
                                request.uri().getHost().equals("login.microsoftonline.com")
                                    ? "{\"access_token\":\"mock-token\",\"expires_in\":3600}" : "");
                    });
            if (status == 403) assertThrows(IllegalArgumentException.class,
                    () -> transport.submitMime(mime, UUID.randomUUID().toString()));
            else assertThrows(IllegalStateException.class,
                    () -> transport.submitMime(mime, UUID.randomUUID().toString()));
        }
    }
}
