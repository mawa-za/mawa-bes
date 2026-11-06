package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Read-only OAuth mailbox retrieval. No IMAP passwords or arbitrary URL configuration. */
@Service @RequiredArgsConstructor
public class BankStatementMailboxService {
    private static final ZoneId CAT = ZoneId.of("Africa/Johannesburg");
    private final BankStatementMailConfigurationService config;
    private final BankTransactionService bankTransactions;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public record SyncResult(int attachments, int newTransactions, int failures) { }
    private record Attachment(String provider, String messageId, String attachmentId, String fileName, byte[] bytes) { }

    public SyncResult sync() {
        var settings = config.get();
        if (!settings.enabled()) throw new IllegalStateException("Bank statement import is disabled for this tenant");
        if (settings.mailbox() == null || settings.sender() == null || settings.provider() == null)
            throw new IllegalStateException("Configure a bank statement mailbox and trusted sender first");
        LocalDate cutoff = LocalDate.now(CAT).minusDays(settings.lookbackDays());
        String bearer = accessToken(settings);
        List<Attachment> attachments = "MICROSOFT_365".equals(settings.provider())
                ? graphAttachments(settings, bearer, cutoff) : gmailAttachments(settings, bearer, cutoff);
        int imported = 0;
        int failures = 0;
        for (Attachment attachment : attachments) {
            try {
                imported += bankTransactions.importCsv(attachment.provider(), attachment.messageId(),
                        attachment.attachmentId(), attachment.fileName(), attachment.bytes(), settings.accountNumbers());
            } catch (RuntimeException ex) {
                failures++;
                // No mailbox content, bank account numbers or OAuth tokens in application logs.
                org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                        "Bank statement attachment import failed (provider={}, attachment index={}, error type={})",
                        settings.provider(), imported + failures, ex.getClass().getSimpleName());
            }
        }
        if (failures == 0) config.recordSuccess(LocalDate.now(CAT).toString());
        return new SyncResult(attachments.size(), imported, failures);
    }

    private String accessToken(BankStatementMailConfigurationService.Configuration c) {
        String secret = config.secret(c.clientSecretRef());
        if (secret == null || secret.isBlank()) throw new IllegalStateException("Mail OAuth client secret not configured");
        String url;
        String body;
        if ("MICROSOFT_365".equals(c.provider())) {
            if (!c.tenantId().matches("[A-Za-z0-9-]{3,100}"))
                throw new IllegalArgumentException("Invalid Microsoft directory tenant ID");
            url = "https://login.microsoftonline.com/" + c.tenantId() + "/oauth2/v2.0/token";
            body = form(Map.of("client_id", c.clientId(), "client_secret", secret,
                    "scope", "https://graph.microsoft.com/.default", "grant_type", "client_credentials"));
        } else if ("GMAIL".equals(c.provider())) {
            String refresh = config.secret(c.refreshTokenRef());
            if (refresh == null || refresh.isBlank()) throw new IllegalStateException("Gmail OAuth refresh token not configured");
            url = "https://oauth2.googleapis.com/token";
            body = form(Map.of("client_id", c.clientId(), "client_secret", secret,
                    "refresh_token", refresh, "grant_type", "refresh_token"));
        } else throw new IllegalArgumentException("Unsupported bank statement mail provider");
        JsonNode response = postForm(url, body);
        String token = response.path("access_token").asText();
        if (token.isBlank()) throw new IllegalStateException("Mailbox OAuth token response did not contain an access token");
        return token;
    }

    private List<Attachment> graphAttachments(BankStatementMailConfigurationService.Configuration c,
                                               String token, LocalDate cutoff) {
        String mailbox = segment(c.mailbox());
        String base = "https://graph.microsoft.com/v1.0/users/" + mailbox + "/messages";
        String filter = "receivedDateTime ge " + cutoff + "T00:00:00Z and hasAttachments eq true";
        String next = base + "?$select=id,subject,from,hasAttachments,receivedDateTime"
                + "&$top=50&$filter=" + encode(filter);
        List<Attachment> result = new ArrayList<>();
        for (int page = 0; next != null && page < 20; page++) {
            if (!next.startsWith("https://graph.microsoft.com/v1.0/"))
                throw new IllegalStateException("Unexpected Graph pagination host");
            JsonNode messages = getJson(next, token);
            for (JsonNode message : messages.path("value")) {
                String sender = message.path("from").path("emailAddress").path("address").asText();
                if (!trusted(c, sender, message.path("subject").asText())) continue;
                String id = message.path("id").asText();
                if (id.isBlank()) continue;
                if (Boolean.TRUE.equals(c.requireDmarc())) {
                    JsonNode messageWithHeaders = getJson(base + "/" + segment(id) + "?$select=internetMessageHeaders", token);
                    if (!dmarcPassed(messageWithHeaders.path("internetMessageHeaders"))) continue;
                }
                String attachmentUrl = base + "/" + segment(id) + "/attachments?$top=100";
                JsonNode details = getJson(attachmentUrl, token);
                for (JsonNode file : details.path("value")) {
                    if (!file.path("@odata.type").asText().endsWith("fileAttachment")) continue;
                    String name = file.path("name").asText();
                    if (!csv(name)) continue;
                    String fileId = file.path("id").asText();
                    String content = file.path("contentBytes").asText();
                    if (!fileId.isBlank() && !content.isBlank() && content.length() < 7_000_000)
                        result.add(new Attachment("MICROSOFT_365", id, fileId, name, Base64.getDecoder().decode(content)));
                }
            }
            next = messages.hasNonNull("@odata.nextLink") ? messages.path("@odata.nextLink").asText() : null;
        }
        return result;
    }

    private List<Attachment> gmailAttachments(BankStatementMailConfigurationService.Configuration c,
                                               String token, LocalDate cutoff) {
        String base = "https://gmail.googleapis.com/gmail/v1/users/me/messages";
        String mailbox = getJson("https://gmail.googleapis.com/gmail/v1/users/me/profile", token)
                .path("emailAddress").asText();
        if (!c.mailbox().equalsIgnoreCase(mailbox))
            throw new IllegalStateException("Gmail OAuth account does not match the configured mailbox");
        String query = "has:attachment after:" + cutoff.toString().replace("-", "/")
                + " from:" + c.sender();
        String pageToken = null;
        List<Attachment> found = new ArrayList<>();
        for (int page = 0; page < 20; page++) {
            String url = base + "?maxResults=100&q=" + encode(query)
                    + (pageToken == null ? "" : "&pageToken=" + encode(pageToken));
            JsonNode messageIds = getJson(url, token);
            for (JsonNode summary : messageIds.path("messages")) {
                String id = summary.path("id").asText();
                if (id.isBlank()) continue;
                JsonNode message = getJson(base + "/" + segment(id) + "?format=full", token);
                String sender = header(message, "From");
                if (!trusted(c, extractEmail(sender), header(message, "Subject"))) continue;
                if (Boolean.TRUE.equals(c.requireDmarc()) &&
                    !dmarcPassed(message.path("payload").path("headers"))) continue;
                collectGmailParts(base, id, message.path("payload"), token, found);
            }
            pageToken = messageIds.path("nextPageToken").asText(null);
            if (pageToken == null || pageToken.isBlank()) break;
        }
        return found;
    }

    private void collectGmailParts(String base, String messageId, JsonNode part,
                                   String token, List<Attachment> found) {
        String name = part.path("filename").asText();
        if (csv(name)) {
            JsonNode body = part.path("body");
            String attachmentId = body.path("attachmentId").asText();
            String data = body.path("data").asText();
            if (!attachmentId.isBlank() && data.isBlank()) {
                data = getJson(base + "/" + segment(messageId) + "/attachments/" + segment(attachmentId), token)
                        .path("data").asText();
            }
            if (!data.isBlank() && data.length() < 7_000_000) {
                String identity = attachmentId.isBlank() ? "inline:" + name : attachmentId;
                found.add(new Attachment("GMAIL", messageId, identity, name, Base64.getUrlDecoder().decode(data)));
            }
        }
        for (JsonNode child : part.path("parts")) collectGmailParts(base, messageId, child, token, found);
    }

    private String header(JsonNode message, String key) {
        for (JsonNode header : message.path("payload").path("headers"))
            if (key.equalsIgnoreCase(header.path("name").asText())) return header.path("value").asText();
        return "";
    }

    private String extractEmail(String value) {
        int start = value.lastIndexOf('<');
        int end = value.lastIndexOf('>');
        return start >= 0 && end > start ? value.substring(start + 1, end).trim() : value.trim();
    }

    private boolean trusted(BankStatementMailConfigurationService.Configuration config,
                            String sender, String subject) {
        return config.sender().equalsIgnoreCase(sender.trim())
                && (config.subjectFilter() == null || config.subjectFilter().isBlank()
                    || subject.toLowerCase(Locale.ROOT).contains(config.subjectFilter().toLowerCase(Locale.ROOT)));
    }

    private boolean dmarcPassed(JsonNode headers) {
        for (JsonNode header : headers) {
            if ("authentication-results".equalsIgnoreCase(header.path("name").asText()) &&
                header.path("value").asText().toLowerCase(Locale.ROOT).matches("(?s).*\\bdmarc=pass\\b.*")) {
                return true;
            }
        }
        return false;
    }

    private boolean csv(String fileName) { return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".csv"); }
    private String segment(String value) { return encode(value).replace("+", "%20"); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String form(Map<String, String> fields) {
        List<String> pairs = new ArrayList<>();
        fields.forEach((key, value) -> pairs.add(encode(key) + "=" + encode(value)));
        return String.join("&", pairs);
    }

    private JsonNode getJson(String url, String token) {
        return send(HttpRequest.newBuilder(URI.create(url)).GET()
                .header("Authorization", "Bearer " + token).header("Accept", "application/json")
                .timeout(Duration.ofSeconds(40)).build());
    }

    private JsonNode postForm(String url, String body) {
        return send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(40))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    private JsonNode send(HttpRequest request) {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                // Do not surface response bodies (they may contain mailbox data or tokens).
                throw new IllegalStateException("Mailbox API returned HTTP " + response.statusCode());
            }
            if (response.body().length > 15_000_000)
                throw new IllegalStateException("Mailbox response exceeded maximum supported size");
            return mapper.readTree(response.body());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to contact mailbox API", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mailbox API request interrupted", ex);
        }
    }
}
