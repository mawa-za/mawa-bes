package za.co.mawa.bes.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BillingEntitlementInterceptor implements HandlerInterceptor {

    private final BillingEntitlementClient entitlementClient;
    private final BillingModuleRouteResolver routeResolver;
    private final ObjectMapper objectMapper;
    private final boolean enforcementEnabled;

    public BillingEntitlementInterceptor(
            BillingEntitlementClient entitlementClient,
            BillingModuleRouteResolver routeResolver,
            ObjectMapper objectMapper,
            @Value("${mawa.billing.entitlement-enforcement-enabled:true}") boolean enforcementEnabled) {
        this.entitlementClient = entitlementClient;
        this.routeResolver = routeResolver;
        this.objectMapper = objectMapper;
        this.enforcementEnabled = enforcementEnabled;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        if (!enforcementEnabled || shouldSkip(request)) {
            return true;
        }

        String moduleCode = routeResolver.resolve(request.getRequestURI());
        if (!StringUtils.hasText(moduleCode)) {
            return true;
        }

        String tenantId = TenantContext.getCurrentTenant();
        if (!StringUtils.hasText(tenantId)) {
            return true;
        }

        if (entitlementClient.isEnabled(tenantId, moduleCode)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "BILLING_MODULE_DISABLED");
        body.put("message", "The " + moduleCode + " module is not enabled for this tenant.");
        body.put("tenantId", tenantId);
        body.put("moduleCode", moduleCode);
        objectMapper.writeValue(response.getWriter(), body);
        return false;
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null
                || path.startsWith("/internal/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.equals("/authenticate")
                || path.equals("/v2/authenticate")
                || path.equals("/forgot-password")
                || path.equals("/v2/forgot-password")
                || path.equals("/reset-password")
                || path.equals("/v2/reset-password")
                || path.equals("/refresh-token")
                || path.equals("/v2/refresh-token");
    }
}
