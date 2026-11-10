package za.co.mawa.bes.configuration.spring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.v2.Microsoft365SaasConnectionService;

import java.io.IOException;

/** Establish tenant context BEFORE entity manager and controllers for Microsoft redirect.
 * State is a tenant schema LOOKUP HINT only; the handler verifies the full random state hash. */
@Component @Order(Ordered.HIGHEST_PRECEDENCE + 19)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MicrosoftBankOAuthCallbackTenantFilter extends OncePerRequestFilter {
    public static final String CALLBACK = "/v2/bank-transactions/microsoft/callback";

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !CALLBACK.equals(request.getRequestURI());
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String state = request.getParameter("state");
        if (!"GET".equalsIgnoreCase(request.getMethod())
                || !Microsoft365SaasConnectionService.validStateFormat(state)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid OAuth authorization state");
            return;
        }
        try {
            TenantContext.setCurrentTenant(state.substring(0, state.indexOf('.')));
            chain.doFilter(request, response);
        } finally { TenantContext.clear(); }
    }
}
