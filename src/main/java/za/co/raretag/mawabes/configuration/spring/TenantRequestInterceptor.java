package za.co.raretag.mawabes.configuration.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import za.co.raretag.mawabes.configuration.context.TenantContext;
import za.co.raretag.mawabes.configuration.security.domain.SecurityDomain;

import java.util.Optional;
import java.util.function.Predicate;

@Component
public class TenantRequestInterceptor implements AsyncHandlerInterceptor {

    private SecurityDomain securityDomain;

    public TenantRequestInterceptor(SecurityDomain securityDomain) {
        this.securityDomain = securityDomain;
    }

    Predicate<String> isPost = it -> it.equals("POST");
    Predicate<String> isAuthenticatePath = it -> it.equals("/authenticate");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        final String method = request.getMethod();
        final String requestURI = request.getRequestURI();
//        if (isPost.test(method) && isAuthenticatePath.test(requestURI)) {
        if (isPost.test(method) && requestURI.contains("/authenticate")){
            String tenantID = request.getHeader("X-TenantID");
            TenantContext.setCurrentTenant(tenantID);
            return true;
        } else {
            return Optional.ofNullable(request)
                    .map(req -> securityDomain.getTenantIdFromJwt(req))
                    .map(tenant -> setTenantContext(tenant))
                    .orElse(false);
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