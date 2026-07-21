package za.co.mawa.bes.configuration.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.security.domain.SecurityDomain;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.exception.TenantNotFound;
import za.co.mawa.bes.exception.TenantNotProvided;
import za.co.mawa.bes.service.TenantAdminService;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TenantRequestInterceptor implements HandlerInterceptor {

    private final TenantAdminService tenantAdminService;
    private final SecurityDomain securityDomain;

    public TenantRequestInterceptor(SecurityDomain securityDomain, TenantAdminService tenantAdminService) {
        this.securityDomain = securityDomain;
        this.tenantAdminService = tenantAdminService;
    }

    Predicate<String> isPost = it -> it.equals("POST");
    Predicate<String> isGet = it -> it.equals("GET");

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws TenantNotFound, TenantNotProvided {

        final String method = request.getMethod();
        final String requestURI = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        if ("/error".equals(requestURI)
                || requestURI.startsWith("/internal/admin/")
                || "/v2/admin-handoff/exchange".equals(requestURI)
                || "/refresh-token".equals(requestURI)
                || "/v2/refresh-token".equals(requestURI)) {
            // Refresh tokens are validated by JwtRefreshService. That service reads
            // the tenant claim and establishes TenantContext before tenant-scoped
            // user lookup. Do not require an access-token tenant at interceptor time.
            return true;
        }

        if (isGet.test(method) && requestURI.contains("/xero/callback")) {
            String tenantReference = firstNonBlank(
                    request.getParameter("state"),
                    request.getHeader("X-TenantID"),
                    request.getHeader("X-Tenant-Id"),
                    request.getHeader("Origin"),
                    request.getHeader("Referer")
            );
            setResolvedTenantContext(tenantReference);
            return true;
        }

        if (requestURI.startsWith("/v2/pos-print-agents/")) {
            String tenantReference = firstNonBlank(
                    request.getHeader("X-TenantID"),
                    request.getHeader("X-Tenant-Id")
            );
            setResolvedTenantContext(tenantReference);
            return true;
        }

        if (isPost.test(method)
                && (requestURI.contains("/authenticate")
                || requestURI.contains("/forgot-password")
                || requestURI.contains("/reset-password"))) {
            String tenantReference = firstNonBlank(
                    request.getHeader("X-TenantID"),
                    request.getHeader("X-Tenant-Id"),
                    request.getHeader("Origin"),
                    request.getHeader("Referer")
            );

            // Local native clients may omit browser Origin/Referer headers.
            if (!StringUtils.hasText(tenantReference)
                    && ("localhost".equalsIgnoreCase(request.getServerName())
                    || "127.0.0.1".equals(request.getServerName()))) {
                tenantReference = TenantContext.LOCALHOST_HOST;
            }

            setResolvedTenantContext(tenantReference);
            return true;
        }

        try {
            return Optional.ofNullable(request)
                    .map(securityDomain::getTenantIdFromJwt)
                    .map(this::setTenantContext)
                    .orElse(false);
        } catch (Exception exception) {
            throw new TenantNotProvided();
        }
    }

    private void setResolvedTenantContext(String tenantReference) throws TenantNotFound, TenantNotProvided {
        if (!StringUtils.hasText(tenantReference)) {
            throw new TenantNotProvided();
        }

        TenantDto tenant = resolveTenant(tenantReference);
        String canonicalHost = firstNonBlank(
                TenantHostNormalizer.normalize(tenant.getHost()),
                TenantHostNormalizer.normalize(tenantReference)
        );

        TenantContext.setCurrentTenant(tenant.getId());
        TenantContext.setCurrentTenantURL(canonicalHost);
    }

    private TenantDto resolveTenant(String tenantReference) throws TenantNotFound {
        String rawReference = tenantReference.trim();
        String normalizedHost = TenantHostNormalizer.normalize(rawReference);
        List<TenantDto> tenants = tenantAdminService.getAll();

        return tenants.stream()
                .filter(tenant -> Objects.equals(tenant.getId(), rawReference)
                        || hostsMatch(tenant.getHost(), normalizedHost))
                .findFirst()
                .orElseThrow(() -> new TenantNotFound(
                        "Tenant not found for host: "
                                + (StringUtils.hasText(normalizedHost) ? normalizedHost : rawReference)
                ));
    }

    private boolean hostsMatch(String configuredHost, String normalizedRequestHost) {
        if (!StringUtils.hasText(normalizedRequestHost)) {
            return false;
        }
        String normalizedConfiguredHost = TenantHostNormalizer.normalize(configuredHost);
        return normalizedRequestHost.equals(normalizedConfiguredHost);
    }

    private boolean setTenantContext(String tenant) {
        TenantContext.setCurrentTenant(tenant);
        return true;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        TenantContext.clear();
    }
}
