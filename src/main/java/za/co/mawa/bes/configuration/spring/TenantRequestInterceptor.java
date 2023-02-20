package za.co.mawa.bes.configuration.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.security.domain.SecurityDomain;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.exception.TenantNotProvided;
import za.co.mawa.bes.service.RemoteTenantService;
import za.co.mawa.bes.service.TenantService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class TenantRequestInterceptor implements AsyncHandlerInterceptor {
    @Autowired
    TenantService tenantService;
    private SecurityDomain securityDomain;

    public TenantRequestInterceptor(SecurityDomain securityDomain) {
        this.securityDomain = securityDomain;
    }

    Predicate<String> isPost = it -> it.equals("POST");
    Predicate<String> isGet = it -> it.equals("GET");
    Predicate<String> isAuthenticatePath = it -> it.equals("/authenticate");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws TenantNotProvided {

        final String method = request.getMethod();
        final String requestURI = request.getRequestURI();
        if (isPost.test(method) && requestURI.contains("/authenticate")) {
            String host = request.getHeader("X-TenantID").split(":")[0];
            List<TenantDto> tenants = tenantService.getAll().stream()
                    .filter(a -> Objects.equals(a.getHost(), host))
                    .toList();
            if (!tenants.isEmpty()) {
                TenantDto tenant = tenants.iterator().next();
                TenantContext.setCurrentTenant(tenant.getName());
                return true;
            } else {
                String tenantID = request.getHeader("X-TenantID");
                if (tenantID != null) {
                    TenantContext.setCurrentTenant(tenantID);
                    return true;
                } else {
                    throw new TenantNotProvided("X-TenantID request header not provided");
                }
            }
        } else {
            try {
                System.out.println(request.toString());
                return Optional.ofNullable(request)
                        .map(req -> securityDomain.getTenantIdFromJwt(req))
                        .map(tenant -> setTenantContext(tenant))
                        .orElse(false);
            } catch (Exception exception) {
                throw new TenantNotProvided();
            }
        }

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        TenantContext.clear();
    }

    private boolean setTenantContext(String tenant) {
        TenantContext.setCurrentTenant(tenant);
        return true;
    }
}