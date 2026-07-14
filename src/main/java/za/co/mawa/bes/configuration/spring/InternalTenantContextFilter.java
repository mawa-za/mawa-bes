package za.co.mawa.bes.configuration.spring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Establishes the tenant before Spring opens an EntityManager for internal
 * administration requests. Setting the tenant only inside a controller is too
 * late when OpenEntityManagerInView is enabled because the Hibernate session
 * may already be bound to the default schema.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InternalTenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_HOST_HEADER = "X-Mawa-Tenant-Host";

    private static final Pattern INTERNAL_TENANT_PATH = Pattern.compile(
            "^/internal/admin/tenant/([^/]+)(?:/.*)?$"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/internal/admin/tenant/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Matcher matcher = INTERNAL_TENANT_PATH.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid internal tenant path");
            return;
        }

        String tenantId = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
        if (!StringUtils.hasText(tenantId)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant is required");
            return;
        }
        tenantId = tenantId.trim();
        if (!tenantId.matches("[A-Za-z0-9_-]{1,128}")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant identifier");
            return;
        }

        try {
            TenantContext.setCurrentTenant(tenantId);
            String tenantHost = request.getHeader(TENANT_HOST_HEADER);
            if (StringUtils.hasText(tenantHost)) {
                TenantContext.setCurrentTenantURL(TenantHostNormalizer.normalize(tenantHost));
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
