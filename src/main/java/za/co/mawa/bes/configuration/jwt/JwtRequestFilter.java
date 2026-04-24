package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.JwtUserDetailsService;
import za.co.mawa.bes.service.UserService;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/authenticate".endsWith(path)
                || "/refresh-token".endsWith(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwtToken = authHeader.substring(7);

                String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                String tenantId = jwtTokenUtil.getTenantIdFromToken(jwtToken);

                TenantContext.setCurrentTenant(tenantId);
                UserContext.setCurrentUser(username);

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

                    if (jwtTokenUtil.validateAccessToken(jwtToken, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        try {
                            UserContext.setCurrentUserPartner(userService.getCurrentUserPartnerId());
                        } catch (Exception e) {
                            log.warn("Unable to resolve partner for user {}", username, e);
                        }
                    }
                }
            }

            chain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT authentication failed: {}", ex.getMessage());
            request.setAttribute("exception", ex);
            chain.doFilter(request, response);
        } finally {
            clearContexts();
        }
    }

    private void clearContexts() {
        try {
            TenantContext.clear();
        } catch (Exception e) {
            log.debug("Unable to clear TenantContext", e);
        }

        try {
            UserContext.clear();
        } catch (Exception e) {
            log.debug("Unable to clear UserContext", e);
        }
    }
}