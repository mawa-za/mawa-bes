package za.co.mawa.bes.configuration.jwt;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtRequestFilterEmergencyBearerTest {
    private static final String TENANT = "phanka-tenant";
    private static final String FUTURE = Instant.now().plusSeconds(3600).toString();

    @AfterEach void cleanup() {
        TenantContext.clear();
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    private JwtRequestFilter filter(boolean enabled) {
        var filter = new JwtRequestFilter();
        var tokens = new JwtTokenUtil();
        tokens.setSecret("mawapay-emergency-token-required-unit-test-secret-at-least-sixty-four-bytes-long");
        tokens.init();
        ReflectionTestUtils.setField(filter, "jwtTokenUtil", tokens);
        ReflectionTestUtils.setField(filter, "emergencyBearerPolicy",
                new MawaPayEmergencyBearerPolicy(enabled, TENANT, FUTURE));
        return filter;
    }

    private MockHttpServletRequest request(String method, String path, String tenant) {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.addHeader("X-TenantID", tenant);
        request.addHeader("Authorization", "Bearer aaa.bbb.ccc");
        // The bearer is intentionally invalid: it should enter the bridge.
        return request;
    }

    @Test void expiredOrInvalidBearerMawaPayReceiptWriteHasAuthenticatedSecurityContextAndTenant() throws Exception {
        var request = request("POST", "/v2/sync/payment-batches/membership-premiums", TENANT);
        var response = new MockHttpServletResponse();
        var seenTenant = new AtomicReference<String>();
        var seenUser = new AtomicReference<String>();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            seenTenant.set(TenantContext.getCurrentTenant());
            seenUser.set(SecurityContextHolder.getContext().getAuthentication().getName());
            assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
            return null;
        }).when(chain).doFilter(any(), any());

        filter(true).doFilter(request, response, chain);

        assertEquals(TENANT, seenTenant.get());
        assertEquals("mawapay-emergency", seenUser.get());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertNull(TenantContext.getCurrentTenant());
        verify(chain).doFilter(any(), any());
    }

    @Test void wrongTenantDoesNotAuthenticate() throws Exception {
        var request = request("GET", "/v2/membership/master-data/snapshot", "different-tenant");
        FilterChain chain = mock(FilterChain.class);
        filter(true).doFilter(request, new MockHttpServletResponse(), chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test void flagDisabledDoesNotAuthenticate() throws Exception {
        var request = request("GET", "/v2/membership/master-data/snapshot", TENANT);
        filter(false).doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void missingBearerNeverAuthenticates() throws Exception {
        var request = new MockHttpServletRequest("GET", "/v2/membership/master-data/snapshot");
        request.setServletPath("/v2/membership/master-data/snapshot");
        request.addHeader("X-TenantID", TENANT);
        filter(true).doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
