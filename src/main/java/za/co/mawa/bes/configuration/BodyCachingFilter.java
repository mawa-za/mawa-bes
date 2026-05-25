package za.co.mawa.bes.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import za.co.mawa.bes.configuration.web.WebSecurityConfig;

import java.io.IOException;

@Component
public class BodyCachingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        for (String excluded : WebSecurityConfig.SWAGGER_WHITELIST) {
            String pattern = excluded.replace("/**", "");

            if (path.startsWith(pattern)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request);

        ContentCachingResponseWrapper wrappedResponse =
                new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }
}