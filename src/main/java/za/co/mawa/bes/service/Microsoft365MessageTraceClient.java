package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Exchange Online Message Trace read-only adapter, independent from Mail.Send permissions.
 * The Graph trace application can see organisation-wide traces: its credentials MUST NOT
 * be exposed to browser/API callers and must only be used by the bounded background worker.
 *
 * This is NOT a webhook. A trace of 'delivered' is Exchange transport evidence, not proof
 * that a human has opened an email. Graph request throttling applies organisation-wide.
 */
@Service
public class Microsoft365MessageTraceClient {
    private static final String TRACE_URL = "https://graph.microsoft.com/v1.0/admin/exchange/tracing/messageTraces";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String senderMailbox;
    private final ObjectMapper mapper;
    private final HttpSender http;
    private volatile String token;
    private volatile Instant tokenUntil = Instant.EPOCH;

    @FunctionalInterface
    interface HttpSender { Reply send(HttpRequest request) throws IOException, InterruptedException; }
    record Reply(int status, String body) { }
    public record Trace(String traceId, String messageId, String sender,
                        String recipient, String status, Instant receivedAt) { }

    @Autowired
    public Microsoft365MessageTraceClient(
            @Value("${mawa.outgoing.m365.trace.tenant-id:}") String tenantId,
            @Value("${mawa.outgoing.m365.trace.client-id:}") String clientId,
            @Value("${mawa.outgoing.m365.trace.client-secret:}") String clientSecret,
            @Value("${mawa.outgoing.m365.mailbox:}") String senderMailbox,
            ObjectMapper mapper) {
        this(tenantId, clientId, clientSecret, senderMailbox, mapper, request -> {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        });
    }

    Microsoft365MessageTraceClient(String tenantId, String clientId, String clientSecret,
                                   String senderMailbox, ObjectMapper mapper, HttpSender http) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.senderMailbox = senderMailbox;
        this.mapper = mapper;
        this.http = http;
    }

    public String approvedSender() { return senderMailbox; }

    /** Bounded exact Message-ID query, never an unfiltered organisational trace export. */
    public List<Trace> findByMessageId(String messageId, Instant acceptedAt) {
        validateConfiguration();
        if (!validMessageId(messageId) || acceptedAt == null) {
            throw new IllegalArgumentException("Invalid Exchange message trace correlation");
        }
        Instant now = Instant.now();
        if (acceptedAt.isAfter(now.plus(5, ChronoUnit.MINUTES))
                || acceptedAt.isBefore(now.minus(89, ChronoUnit.DAYS))) {
            throw new IllegalArgumentException("Exchange message trace timestamp is out of range");
        }
        Instant start = acceptedAt.minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = acceptedAt.plus(4, ChronoUnit.DAYS).isBefore(now)
                ? acceptedAt.plus(4, ChronoUnit.DAYS) : now;
        if (end.isBefore(start)) end = now;
        String filter = "messageId eq '" + escapeOData(messageId) + "'"
                + " and senderAddress eq '" + escapeOData(senderMailbox) + "'"
                + " and receivedDateTime ge " + start.truncatedTo(ChronoUnit.SECONDS)
                + " and receivedDateTime le " + end.truncatedTo(ChronoUnit.SECONDS);
        String uri = TRACE_URL + "?$top=50&$filter="
                + URLEncoder.encode(filter, StandardCharsets.UTF_8).replace("+", "%20");
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + accessToken())
                .header("Accept", "application/json")
                .GET().build();
        Reply response = exchange(request);
        if (response.status() != 200) {
            throw new IllegalStateException("Microsoft 365 message trace unavailable (HTTP "
                    + response.status() + ")");
        }
        try {
            JsonNode root = mapper.readTree(response.body());
            JsonNode values = root.path("value");
            if (!values.isArray() || values.size() > 50 || root.hasNonNull("@odata.nextLink")) {
                // Fail closed rather than silently accepting an incomplete page of matches.
                throw new IllegalStateException("Ambiguous or truncated Exchange message trace result");
            }
            List<Trace> traces = new ArrayList<>();
            for (JsonNode item : values) {
                String id = item.path("id").asText("");
                String msg = item.path("messageId").asText("");
                String from = item.path("senderAddress").asText("");
                String recipient = item.path("recipientAddress").asText("");
                String status = item.path("status").asText("");
                String received = item.path("receivedDateTime").asText("");
                if (id.length() > 80 || id.isBlank() || !validMessageId(msg)
                        || !StringUtils.hasText(from) || !StringUtils.hasText(recipient)
                        || !StringUtils.hasText(status) || !StringUtils.hasText(received)) continue;
                try {
                    traces.add(new Trace(id, msg, from, recipient, status, Instant.parse(received)));
                } catch (RuntimeException ignored) {
                    // One malformed record must not result in an inferred delivery confirmation.
                }
            }
            return List.copyOf(traces);
        } catch (IOException ex) {
            throw new IllegalStateException("Malformed Microsoft 365 trace response", ex);
        }
    }

    private void validateConfiguration() {
        if (!validUuid(tenantId) || !validUuid(clientId) || !StringUtils.hasText(clientSecret)
                || !StringUtils.hasText(senderMailbox)
                || !senderMailbox.equals(SharedOutgoingEmailService.validatedSingleAddress(senderMailbox))) {
            throw new IllegalStateException("Microsoft 365 message tracing has not been configured");
        }
    }

    private synchronized String accessToken() {
        if (token != null && Instant.now().isBefore(tokenUntil)) return token;
        String form = "client_id=" + encode(clientId) + "&client_secret=" + encode(clientSecret)
                + "&scope=" + encode("https://graph.microsoft.com/.default")
                + "&grant_type=client_credentials";
        HttpRequest req = HttpRequest.newBuilder(URI.create(
                    "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        Reply result = exchange(req);
        if (result.status() != 200) {
            throw new IllegalStateException("Microsoft 365 trace token rejected (HTTP " + result.status() + ")");
        }
        try {
            JsonNode json = mapper.readTree(result.body());
            String newToken = json.path("access_token").asText("");
            if (!StringUtils.hasText(newToken)) throw new IllegalStateException("Trace token unavailable");
            int seconds = json.path("expires_in").asInt(3600);
            token = newToken;
            tokenUntil = Instant.now().plusSeconds(Math.max(1, seconds - 120));
            return token;
        } catch (IOException ex) {
            throw new IllegalStateException("Invalid Microsoft 365 trace token response", ex);
        }
    }

    private Reply exchange(HttpRequest request) {
        try { return http.send(request); }
        catch (IOException ex) { throw new IllegalStateException("Microsoft 365 trace connection failure", ex); }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Microsoft 365 trace request interrupted", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    private static String escapeOData(String value) { return value.replace("'", "''"); }
    private static boolean validUuid(String value) {
        return value != null && value.matches("[0-9a-fA-F-]{36}");
    }
    static boolean validMessageId(String value) {
        return value != null && value.length() <= 255 && value.length() >= 5
                && value.indexOf('\r') < 0 && value.indexOf('\n') < 0
                && value.startsWith("<") && value.endsWith(">");
    }
}
