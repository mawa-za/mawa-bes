package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;

/**
 * App-only Microsoft 365 Exchange Online transport. Only the centrally owned sending
 * mailbox is permitted: the sender on a tenant-composed MIME message must match it.
 *
 * Grant Application Mail.Send with Exchange Online application RBAC scoped to this mailbox.
 * DO NOT grant unscoped Graph Mail.Send in Entra: permissions are additive.
 * HTTP 202 means Graph accepted the request, not that the recipient received it.
 */
@Service
public class Microsoft365GraphOutgoingTransport {
    private static final String GRAPH = "https://graph.microsoft.com/v1.0/users/";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();
    private final String directoryId;
    private final String clientId;
    private final String secret;
    private final String mailbox;
    private final ObjectMapper mapper;
    private final HttpSender sender;

    @FunctionalInterface
    interface HttpSender {
        Response send(HttpRequest request) throws IOException, InterruptedException;
    }
    record Response(int status, String body) {}
    /** Permanent mistakes must not be retried by the outbox worker. */
    public static final class PermanentMailConfigurationException extends IllegalArgumentException {
        public PermanentMailConfigurationException(String message) { super(message); }
    }

    private volatile String cachedToken;
    private volatile Instant tokenValidUntil = Instant.EPOCH;

    @Autowired
    public Microsoft365GraphOutgoingTransport(
            @Value("${mawa.outgoing.m365.tenant-id:}") String directoryId,
            @Value("${mawa.outgoing.m365.client-id:}") String clientId,
            @Value("${mawa.outgoing.m365.client-secret:}") String secret,
            @Value("${mawa.outgoing.m365.mailbox:}") String mailbox,
            ObjectMapper mapper) {
        this(directoryId, clientId, secret, mailbox, mapper, request -> {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        });
    }

    Microsoft365GraphOutgoingTransport(String directoryId, String clientId, String secret,
                                       String mailbox, ObjectMapper mapper, HttpSender sender) {
        this.directoryId = directoryId;
        this.clientId = clientId;
        this.secret = secret;
        this.mailbox = mailbox;
        this.mapper = mapper;
        this.sender = sender;
    }

    /** Returns the client MIME Message-ID for audit correlation, not a Graph provider message ID. */
    public String submitMime(byte[] rawMime, String requestId) {
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())) {
            throw new IllegalStateException("Tenant context is required for Microsoft 365 outgoing mail");
        }
        validateConfiguration();
        if (rawMime == null || rawMime.length == 0 || rawMime.length > 10 * 1024 * 1024) {
            throw new PermanentMailConfigurationException("Invalid outgoing MIME payload size");
        }
        if (requestId == null || !requestId.matches("[0-9a-fA-F-]{36}")) {
            throw new PermanentMailConfigurationException("Invalid outgoing email request ID");
        }
        final String clientMessageId;
        try {
            MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(rawMime));
            if (mime.getFrom() == null || mime.getFrom().length != 1
                    || !(mime.getFrom()[0] instanceof InternetAddress address)
                    || !mailbox.equalsIgnoreCase(address.getAddress())) {
                throw new PermanentMailConfigurationException(
                        "Outgoing MIME From must match the approved Microsoft 365 mailbox");
            }
            if (mime.getAllRecipients() == null || mime.getAllRecipients().length == 0) {
                throw new PermanentMailConfigurationException("Outgoing email requires recipients");
            }
            clientMessageId = mime.getMessageID();
        } catch (jakarta.mail.MessagingException ex) {
            throw new PermanentMailConfigurationException("Invalid outgoing MIME message");
        }

        String token = accessToken();
        HttpRequest request = HttpRequest.newBuilder(
                    URI.create(GRAPH + URLEncoder.encode(mailbox, StandardCharsets.UTF_8)
                            + "/sendMail"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(
                        Base64.getEncoder().encodeToString(rawMime), StandardCharsets.US_ASCII))
                .build();
        Response response = exchange(request);
        if (response.status() == 202) return clientMessageId;
        if (response.status() == 400 || response.status() == 401 || response.status() == 403
                || response.status() == 404 || response.status() == 413 || response.status() == 422) {
            // Never log Graph's response body; it may include recipient or mailbox information.
            throw new PermanentMailConfigurationException(
                    "Microsoft 365 Graph rejected outgoing mail (HTTP " + response.status() + ")");
        }
        // Includes 429 and 5xx: outbox worker will retry with bounded backoff.
        throw new IllegalStateException("Microsoft 365 Graph delivery submission failed (HTTP "
                + response.status() + ")");
    }

    private void validateConfiguration() {
        if (directoryId == null || !directoryId.matches("[0-9a-fA-F-]{36}")
                || clientId == null || !clientId.matches("[0-9a-fA-F-]{36}")
                || !StringUtils.hasText(secret) || !StringUtils.hasText(mailbox)
                || !mailbox.equals(SharedOutgoingEmailService.validatedSingleAddress(mailbox))) {
            throw new PermanentMailConfigurationException("Microsoft 365 outgoing credentials or mailbox not configured");
        }
    }

    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenValidUntil)) return cachedToken;
        String form = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(secret)
                + "&scope=" + encode("https://graph.microsoft.com/.default")
                + "&grant_type=client_credentials";
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                    "https://login.microsoftonline.com/" + directoryId + "/oauth2/v2.0/token"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        Response response = exchange(request);
        if (response.status() != 200) {
            if (response.status() >= 400 && response.status() < 500 && response.status() != 429) {
                throw new PermanentMailConfigurationException(
                        "Microsoft 365 token request rejected (HTTP " + response.status() + ")");
            }
            throw new IllegalStateException("Microsoft 365 token request unavailable (HTTP "
                    + response.status() + ")");
        }
        try {
            JsonNode json = mapper.readTree(response.body());
            String value = json.path("access_token").asText();
            long ttl = json.path("expires_in").asLong(3600);
            if (!StringUtils.hasText(value) || ttl < 30) {
                throw new IllegalStateException("Invalid Microsoft 365 access token response");
            }
            cachedToken = value;
            tokenValidUntil = Instant.now().plusSeconds(Math.max(1, ttl - 90));
            return value;
        } catch (IOException ex) {
            throw new IllegalStateException("Invalid Microsoft 365 token response", ex);
        }
    }

    private Response exchange(HttpRequest request) {
        try {
            return sender.send(request);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Microsoft 365 request interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Microsoft 365 request unavailable", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
