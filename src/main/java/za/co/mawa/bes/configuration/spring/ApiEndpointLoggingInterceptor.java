package za.co.mawa.bes.configuration.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.service.v2.ApiEndpointLogService;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiEndpointLoggingInterceptor implements HandlerInterceptor {

    private final ApiEndpointLogService logService;

    private static final String START_TIME_ATTRIBUTE = "apiLogStartTime";
    private static final String REQUEST_ID_ATTRIBUTE = "apiLogRequestId";

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

        boolean success = response.getStatus() < 400 && ex == null;
        log.setSuccess(success);

        if (ex != null) {
            log.setErrorMessage(ex.getMessage());
        }

        // Optional: populate from your UserContext
        log.setUserId(getCurrentUserIdSafe());
        log.setUsername(getCurrentUsernameSafe());

        logService.saveAsync(log);
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
            // return UserContext.getUserId();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUsernameSafe() {
        try {
            // return UserContext.getUsername();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}