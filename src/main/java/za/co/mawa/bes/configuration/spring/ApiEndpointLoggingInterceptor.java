package za.co.mawa.bes.configuration.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.service.v2.ApiEndpointLogService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiEndpointLoggingInterceptor implements HandlerInterceptor {

    private final ApiEndpointLogService logService;

    private static final String START_TIME_ATTRIBUTE = "apiLogStartTime";
    private static final String REQUEST_ID_ATTRIBUTE = "apiLogRequestId";

    private static final int MAX_BODY_LENGTH = 10000;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        request.setAttribute(REQUEST_ID_ATTRIBUTE, UUID.randomUUID().toString());

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        String requestId = (String) request.getAttribute(REQUEST_ID_ATTRIBUTE);

        long durationMs = 0L;

        if (startTime != null) {
            durationMs = System.currentTimeMillis() - startTime;
        }

        ApiEndpointLogEntity log = new ApiEndpointLogEntity();
        log.setRequestId(requestId);
        log.setMethod(request.getMethod());
        log.setEndpoint(request.getRequestURI());
        log.setQueryString(request.getQueryString());
        log.setStatusCode(response.getStatus());
        log.setRequestIp(getClientIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setDurationMs(durationMs);
        log.setCreatedAt(LocalDateTime.now());

        if (shouldHideBody(request.getRequestURI())) {
            log.setRequestBody("[HIDDEN]");
            log.setResponseBody("[HIDDEN]");
        } else {
            log.setRequestBody(limitBody(getRequestBody(request)));
            log.setResponseBody(limitBody(getResponseBody(response)));
        }

        boolean success = response.getStatus() < 400 && ex == null;
        log.setSuccess(success);

        if (ex != null) {
            log.setErrorMessage(ex.getMessage());
        }

        log.setUserId(getCurrentUserIdSafe());
        log.setUsername(getCurrentUsernameSafe());

        logService.saveAsync(log);
    }

    private String getRequestBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }

        byte[] content = wrapper.getContentAsByteArray();

        if (content.length == 0) {
            return null;
        }

        return new String(content, StandardCharsets.UTF_8);
    }

    private String getResponseBody(HttpServletResponse response) {
        if (!(response instanceof ContentCachingResponseWrapper wrapper)) {
            return null;
        }

        byte[] content = wrapper.getContentAsByteArray();

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
        return path.contains("/auth")
                || path.contains("/authenticate")
                || path.contains("/forgot-password")
                || path.contains("/refresh-token")
                || path.contains("/password")
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
            return UserContext.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUsernameSafe() {
        try {
            return UserContext.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}