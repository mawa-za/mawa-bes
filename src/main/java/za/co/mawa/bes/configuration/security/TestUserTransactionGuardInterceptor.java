package za.co.mawa.bes.configuration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import za.co.mawa.bes.service.UserAccessService;

import java.util.Map;

@Component
public class TestUserTransactionGuardInterceptor implements HandlerInterceptor {

    @Autowired
    private UserAccessService accessService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(request.getServletPath())) {
            return true;
        }

        try {
            accessService.validateCurrentSession();
        } catch (SecurityException exception) {
            return deny(response, "ACCESS_EXPIRED", exception.getMessage());
        }

        if (isExternalMutation(request) && accessService.externalTransactionsBlocked()) {
            accessService.audit(
                    "EXTERNAL_TRANSACTION_BLOCKED",
                    "ENDPOINT",
                    request.getServletPath(),
                    "Test-user policy",
                    request.getMethod()
            );
            return deny(
                    response,
                    "TEST_USER_EXTERNAL_TRANSACTION_BLOCKED",
                    "Testing users cannot execute this external transaction in the current environment."
            );
        }
        return true;
    }

    private boolean isExternalMutation(HttpServletRequest request) {
        if ("GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getServletPath().toLowerCase();
        return path.contains("/xero")
                || path.contains("/fnb")
                || path.contains("bank-payment")
                || path.contains("bank-report")
                || path.contains("submit-to-bank")
                || path.contains("/secret")
                || path.contains("supplier-disbursement")
                || path.contains("refund-execute");
    }

    private boolean isPublic(String path) {
        if (path == null) {
            return true;
        }
        return path.equals("/authenticate")
                || path.equals("/v2/authenticate")
                || path.equals("/forgot-password")
                || path.equals("/v2/forgot-password")
                || path.equals("/reset-password")
                || path.equals("/v2/reset-password")
                || path.equals("/refresh-token")
                || path.equals("/v2/refresh-token")
                || path.equals("/v2/company-logo/content")
                || path.equals("/v2/admin-handoff/exchange")
                || path.equals("/xero/callback")
                || path.startsWith("/internal/admin/")
                || path.startsWith("/v2/pos-print-agents/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }

    private boolean deny(HttpServletResponse response, String code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(mapper.writeValueAsString(Map.of("code", code, "message", message)));
        return false;
    }
}
