package za.co.mawa.bes.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import za.co.mawa.bes.configuration.web.WebSecurityConfig;

import java.io.IOException;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class BodyCachingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUEST_BODY_CACHE_BYTES = 16 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        for (String excluded : WebSecurityConfig.SWAGGER_WHITELIST) {
            String pattern = excluded.replace("/**", "");
            if (path.startsWith(pattern)) {
                return true;
            }
        }

        return path.startsWith("/v2/membership/master-data")
                || path.contains("/content")
                || path.contains("/download")
                || path.contains("/report")
                || path.contains("/pdf");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Response caching previously retained complete downloads and large JSON
        // payloads in heap. Only cache a bounded request prefix for diagnostics.
        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request, MAX_REQUEST_BODY_CACHE_BYTES);
        filterChain.doFilter(wrappedRequest, response);
    }
}
