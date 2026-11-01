package za.co.mawa.bes.controller.v2;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.configuration.jwt.JwtRefreshService;
import za.co.mawa.bes.configuration.jwt.MawaPayEmergencyBearerPolicy;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenControllerV2EmergencyBearerTest {
    private static final String TENANT = "phanka-tenant";

    private RefreshTokenControllerV2 controller(boolean enabled) {
        var controller = new RefreshTokenControllerV2();
        JwtRefreshService refresh = mock(JwtRefreshService.class);
        when(refresh.refresh("aaa.bbb.ccc")).thenThrow(new JwtException("invalid"));
        ReflectionTestUtils.setField(controller, "jwtRefreshService", refresh);
        ReflectionTestUtils.setField(controller, "emergencyBearerPolicy",
                new MawaPayEmergencyBearerPolicy(enabled, TENANT,
                        Instant.now().plusSeconds(3600).toString()));
        return controller;
    }

    private MockHttpServletRequest request(String tenant) {
        var request = new MockHttpServletRequest("POST", "/v2/refresh-token");
        request.setServletPath("/v2/refresh-token");
        request.addHeader("X-TenantID", tenant);
        request.addHeader("Authorization", "Bearer aaa.bbb.ccc");
        return request;
    }

    @Test void invalidRefreshDuringEmergencyDoesNotReturnTokenClearing401() throws Exception {
        var response = controller(true).tokenRefresh(request(TENANT));
        assertEquals(503, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
    }

    @Test void otherTenantAndDisabledFlagKeepUsual401() throws Exception {
        assertEquals(401, controller(true).tokenRefresh(request("other-tenant")).getStatusCode().value());
        assertEquals(401, controller(false).tokenRefresh(request(TENANT)).getStatusCode().value());
    }
}
