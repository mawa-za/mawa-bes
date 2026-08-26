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

        // The JWT servlet filter establishes TenantContext before MVC
        // interceptors run. Avoid decoding the same bearer token again on every
        // authenticated request when the tenant is already known.
        if (StringUtils.hasText(TenantContext.getCurrentTenant())) {
            return true;
        }

        if ("/error".equals(requestURI)
                || requestURI.startsWith("/internal/admin/")
                || "/v2/admin-handoff/exchange".equals(requestURI)
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
        String normalizedReference = TenantHostNormalizer.normalize(tenantReference);
        boolean explicitTenantId = Objects.equals(tenant.getId(), tenantReference.trim());
        String canonicalHost = explicitTenantId
                ? firstNonBlank(
                        TenantHostNormalizer.normalize(tenant.getHost()),
                        TenantHostNormalizer.normalize(tenant.getUrl()))
                : firstNonBlank(
                        normalizedReference,
                        TenantHostNormalizer.normalize(tenant.getHost()),
                        TenantHostNormalizer.normalize(tenant.getUrl()));

        TenantContext.setCurrentTenant(tenant.getId());
        TenantContext.setCurrentTenantURL(canonicalHost);
    }

    private TenantDto resolveTenant(String tenantReference) throws TenantNotFound {
        String rawReference = tenantReference.trim();
        String normalizedHost = TenantHostNormalizer.normalize(rawReference);
        List<TenantDto> tenants = tenantAdminService.getAll();

        Optional<TenantDto> directMatch = tenants.stream()
                .filter(tenant -> Objects.equals(tenant.getId(), rawReference)
                        || hostsMatch(tenant.getHost(), normalizedHost))
                .findFirst();
        if (directMatch.isPresent()) {
            return directMatch.get();
        }

        // Some existing tenant records keep the browser-facing ERP address in
        // url/erpAppUrl while host contains an older alias. Accept the URL only
        // when it identifies exactly one tenant; never guess between tenants
        // that share a common application address.
        List<TenantDto> urlMatches = tenants.stream()
                .filter(tenant -> hostsMatch(tenant.getUrl(), normalizedHost))
                .filter(distinctTenantId())
                .toList();
        if (urlMatches.size() == 1) {
            return urlMatches.get(0);
        }
        if (urlMatches.size() > 1) {
            throw new TenantNotFound(
                    "More than one tenant uses this MAWA address. Open the tenant-specific link or provide the tenant ID"
            );
        }

        throw new TenantNotFound(
                "Tenant not found for host: "
                        + (StringUtils.hasText(normalizedHost) ? normalizedHost : rawReference)
        );
    }

    private Predicate<TenantDto> distinctTenantId() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        return tenant -> tenant != null && StringUtils.hasText(tenant.getId()) && seen.add(tenant.getId());
    }

    private boolean hostsMatch(String configuredHost, String normalizedRequestHost) {
        if (!StringUtils.hasText(configuredHost) || !StringUtils.hasText(normalizedRequestHost)) {
            return false;
        }
        for (String alias : configuredHost.split(";")) {
            String normalizedConfiguredHost = TenantHostNormalizer.normalize(alias);
            if (normalizedRequestHost.equals(normalizedConfiguredHost)) {
                return true;
            }
        }
        return false;
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
