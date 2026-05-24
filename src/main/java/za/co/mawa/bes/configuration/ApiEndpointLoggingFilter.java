package za.co.mawa.bes.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.service.v2.ApiEndpointLogService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiEndpointLoggingFilter extends OncePerRequestFilter {

    private final ApiEndpointLogService logService;

    private static final int MAX_BODY_LENGTH = 10000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/api-endpoint-logs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String requestId = UUID.randomUUID().toString();

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        Exception capturedException = null;

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception ex) {
            capturedException = ex;
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            ApiEndpointLogEntity log = new ApiEndpointLogEntity();
            log.setRequestId(requestId);
            log.setMethod(request.getMethod());
            log.setEndpoint(request.getRequestURI());
            log.setQueryString(request.getQueryString());
            log.setStatusCode(wrappedResponse.getStatus());
            log.setRequestIp(getClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setDurationMs(durationMs);
            log.setCreatedAt(LocalDateTime.now());

            if (shouldHideBody(request.getRequestURI())) {
                log.setRequestBody("[HIDDEN]");
                log.setResponseBody("[HIDDEN]");
            } else {
                log.setRequestBody(limitBody(getRequestBody(wrappedRequest)));
                log.setResponseBody(limitBody(getResponseBody(wrappedResponse)));
            }

            boolean success = wrappedResponse.getStatus() < 400 && capturedException == null;
            log.setSuccess(success);

            if (capturedException != null) {
                log.setErrorMessage(capturedException.getMessage());
            }

            // Optional: connect this to your existing UserContext
            log.setUserId(getCurrentUserIdSafe());
            log.setUsername(getCurrentUsernameSafe());

            logService.save(log);

            wrappedResponse.copyBodyToResponse();
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();

        if (content.length == 0) {
            return null;
        }

        return new String(content, StandardCharsets.UTF_8);
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();

        if (content.length == 0) {
            return null;
        }

        return new String(content, StandardCharsets.UTF_8);
    }

    private String limitBody(String body) {
        if (body == null) {
            return null;
        }

        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }

        return body.substring(0, MAX_BODY_LENGTH) + "... [TRUNCATED]";
    }

    private boolean shouldHideBody(String path) {
        return path.contains("/authenticate")
                || path.contains("/change-password")
                || path.contains("/bank")
                || path.contains("/payment");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String getCurrentUserIdSafe() {
        try {
            // Replace with your existing UserContext if available
            // return UserContext.getUserId();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUsernameSafe() {
        try {
            // Replace with your existing UserContext if available
            // return UserContext.getUsername();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
