package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Microsoft365MessageTraceClientTest {
    private static final String DIRECTORY = "12345678-1234-1234-1234-123456789012";
    private static final String APP = "abcdefab-abcd-abcd-abcd-abcdefabcdef";
    private static final String MAILBOX = "noreply@mawa.co.za";
    private static final String MESSAGE_ID = "<f6ea5bd9@test.mawa.co.za>";

    @Test void filtersByExactMessageIdAndApprovedMailboxWithoutUnboundedTraceScan() {
        List<HttpRequest> requests = new ArrayList<>();
        Microsoft365MessageTraceClient client = new Microsoft365MessageTraceClient(
                DIRECTORY, APP, "qa-client-secret", MAILBOX, new ObjectMapper(), request -> {
            requests.add(request);
            if (request.uri().getHost().equals("login.microsoftonline.com"))
                return new Microsoft365MessageTraceClient.Reply(200,
                        "{\"access_token\":\"mock-oauth-token\",\"expires_in\":3600}");
            return new Microsoft365MessageTraceClient.Reply(200, """
                    {"value":[{"id":"0ec05c1a-d133-4ab1-a20f-7f18ee078198",
                    "senderAddress":"noreply@mawa.co.za","recipientAddress":"qa@example.org",
                    "messageId":"<f6ea5bd9@test.mawa.co.za>","status":"delivered",
                    "receivedDateTime":"2026-11-14T11:00:00Z"}]}
                    """);
        });
        List<Microsoft365MessageTraceClient.Trace> result = client.findByMessageId(MESSAGE_ID,
                Instant.now().minusSeconds(3600));
        assertEquals(1, result.size());
        assertEquals("delivered", result.get(0).status());
        assertEquals("graph.microsoft.com", requests.get(1).uri().getHost());
        assertTrue(requests.get(1).uri().getRawQuery().contains("messageId"));
        assertTrue(requests.get(1).uri().getRawQuery().contains("senderAddress"));
        assertTrue(requests.get(1).uri().getRawQuery().contains("receivedDateTime"));
        assertTrue(requests.get(1).uri().getRawQuery().contains("%3C"));
        assertTrue(requests.get(1).uri().toString().contains("$top=50"));
        client.findByMessageId(MESSAGE_ID, Instant.now().minusSeconds(3600));
        assertEquals(3, requests.size()); // OAuth token reused.
    }

    @Test void badCorrelationCannotAccessGraph() {
        Microsoft365MessageTraceClient client = new Microsoft365MessageTraceClient(
                DIRECTORY, APP, "qa-secret", MAILBOX, new ObjectMapper(), request -> {
            fail("No Microsoft request expected"); return null;
        });
        assertThrows(IllegalArgumentException.class,
                () -> client.findByMessageId("not-a-message-id", Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> client.findByMessageId(MESSAGE_ID, Instant.now().minusSeconds(100L * 86400)));
    }

    @Test void missingTraceAppCredentialsFailClosed() {
        Microsoft365MessageTraceClient client = new Microsoft365MessageTraceClient(
                DIRECTORY, APP, "", MAILBOX, new ObjectMapper(), request -> {
            fail("No Microsoft request expected"); return null;
        });
        assertThrows(IllegalStateException.class,
                () -> client.findByMessageId(MESSAGE_ID, Instant.now()));
    }

    @Test void rejectsGraph403AndTruncatedResponse() {
        Microsoft365MessageTraceClient forbidden = new Microsoft365MessageTraceClient(
                DIRECTORY, APP, "qa-secret", MAILBOX, new ObjectMapper(), request -> {
            return new Microsoft365MessageTraceClient.Reply(
                    request.uri().getHost().equals("login.microsoftonline.com") ? 200 : 403,
                    "{\"access_token\":\"test-token\",\"expires_in\":3600}");
        });
        assertThrows(IllegalStateException.class,
                () -> forbidden.findByMessageId(MESSAGE_ID, Instant.now()));
        Microsoft365MessageTraceClient truncated = new Microsoft365MessageTraceClient(
                DIRECTORY, APP, "qa-secret", MAILBOX, new ObjectMapper(), request -> {
            return new Microsoft365MessageTraceClient.Reply(200,
                    request.uri().getHost().equals("login.microsoftonline.com")
                            ? "{\"access_token\":\"test-token\"}"
                            : "{\"value\":[],\"@odata.nextLink\":\"https://graph.microsoft.com/next\"}");
        });
        assertThrows(IllegalStateException.class,
                () -> truncated.findByMessageId(MESSAGE_ID, Instant.now()));
    }
}
