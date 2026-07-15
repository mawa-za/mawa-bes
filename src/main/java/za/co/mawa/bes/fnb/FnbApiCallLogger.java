package za.co.mawa.bes.fnb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.service.v2.ApiEndpointLogService;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FnbApiCallLogger {

    private static final int MAX_BODY_LENGTH = 20_000;
    private static final Pattern SECRET_JSON_VALUE = Pattern.compile(
            "(?i)(\\\"(?:access_token|refresh_token|client_secret|clientSecret|authorization)\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")"
    );
    private static final Pattern ACCOUNT_JSON_VALUE = Pattern.compile(
            "(?i)(\\\"(?:accountNumber|debtorAccountNumber)\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")"
    );

    private final ApiEndpointLogService apiEndpointLogService;

    public String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public void logCall(
            String requestId,
            String method,
            String endpoint,
            String requestBody,
            Integer statusCode,
            String responseBody,
            long durationMs,
            Throwable failure
    ) {
        try {
            boolean success = failure == null
                    && statusCode != null
                    && statusCode >= 200
                    && statusCode < 300;

            ApiEndpointLogEntity endpointLog = ApiEndpointLogEntity.builder()
                    .requestId(requestId == null || requestId.isBlank() ? newRequestId() : requestId)
                    .direction("OUTBOUND")
                    .integrationName("FNB")
                    .username("FNB Integration")
                    .method(method)
                    .endpoint(endpoint == null || endpoint.isBlank() ? "FNB" : endpoint)
                    .statusCode(statusCode)
                    .requestIp("OUTBOUND")
                    .userAgent("MAWA FNB API Client")
                    .requestBody(sanitizeAndLimit(requestBody))
                    .responseBody(sanitizeAndLimit(responseBody))
                    .durationMs(durationMs)
                    .success(success)
                    .errorMessage(failure == null ? null : sanitizeAndLimit(rootMessage(failure)))
                    .createdAt(LocalDateTime.now())
                    .build();

            apiEndpointLogService.saveAsync(endpointLog);
        } catch (Exception e) {
            // API logging must never make a successful bank call fail.
            log.error("Failed to queue FNB API activity log for {} {}", method, endpoint, e);
        }
    }

    public String tokenRequestSummary() {
        return "grant_type=client_credentials&scope=i_can&client_credentials=[REDACTED]";
    }

    private String sanitizeAndLimit(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        String sanitized = replaceAll(SECRET_JSON_VALUE, body, value -> "[REDACTED]");
        sanitized = replaceAll(ACCOUNT_JSON_VALUE, sanitized, this::maskAccountNumber);

        if (sanitized.length() <= MAX_BODY_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_BODY_LENGTH) + "... [TRUNCATED]";
    }

    private String replaceAll(Pattern pattern, String source, ValueSanitizer sanitizer) {
        Matcher matcher = pattern.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1)
                    + sanitizer.sanitize(matcher.group(2))
                    + matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String maskAccountNumber(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    @FunctionalInterface
    private interface ValueSanitizer {
        String sanitize(String value);
    }
}
