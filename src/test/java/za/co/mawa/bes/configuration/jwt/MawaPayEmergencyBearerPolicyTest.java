package za.co.mawa.bes.configuration.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MawaPayEmergencyBearerPolicyTest {
    private static final String TENANT = "phanka-tenant";
    private static final String FUTURE = Instant.now().plusSeconds(3600).toString();

    private MockHttpServletRequest request(String method, String path, String tenant) {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (tenant != null) request.addHeader("X-TenantID", tenant);
        request.addHeader("Authorization", "Bearer aaa.bbb.ccc");
        return request;
    }

    @Test void expiredOrUnverifiedBearerJwtPermitsApprovedReadAndWriteRoutes() {
        var policy = new MawaPayEmergencyBearerPolicy(true, TENANT, FUTURE);
        assertEquals(TENANT, policy.approvedTenant(request("GET", "/v2/membership/master-data/snapshot", TENANT)));
        assertEquals(TENANT, policy.approvedTenant(request("POST", "/v2/sync/payment-batches/membership-premiums", TENANT)));
        assertEquals(TENANT, policy.approvedTenant(request("POST", "/pay-app/receipt-sync", TENANT)));
        assertEquals(TENANT, policy.approvedTenant(request("POST", "/v2/number-allocation/allocate", TENANT)));
    }

    @Test void disabledOrExpiredConfigurationDoesNotBypassAuthentication() {
        var request = request("GET", "/v2/membership/master-data/snapshot", TENANT);
        assertNull(new MawaPayEmergencyBearerPolicy(false, TENANT, FUTURE).approvedTenant(request));
        assertNull(new MawaPayEmergencyBearerPolicy(true, TENANT, "invalid").approvedTenant(request));
        assertNull(new MawaPayEmergencyBearerPolicy(true, TENANT, Instant.now().minusSeconds(1).toString()).approvedTenant(request));
        assertNull(new MawaPayEmergencyBearerPolicy(true, "", FUTURE).approvedTenant(request));
    }

    @Test void otherTenantsAndOtherPathsNeverBypass() {
        var policy = new MawaPayEmergencyBearerPolicy(true, TENANT, FUTURE);
        assertNull(policy.approvedTenant(request("GET", "/v2/membership/master-data/snapshot", "other-tenant")));
        assertNull(policy.approvedTenant(request("GET", "/v2/membership/master-data/snapshot", null)));
        assertNull(policy.approvedTenant(request("GET", "/v2/admin/users", TENANT)));
        assertNull(policy.approvedTenant(request("POST", "/v2/pay-app/device-identity/enroll", TENANT)));
        assertNull(policy.approvedTenant(request("POST", "/v2/pay-app/device-identity/renew", TENANT)));
        assertNull(policy.approvedTenant(request("DELETE", "/v2/membership/123", TENANT)));
    }

    @Test void missingOrNonJwtBearerNeverBypasses() {
        var policy = new MawaPayEmergencyBearerPolicy(true, TENANT, FUTURE);
        var request = new MockHttpServletRequest("GET", "/v2/membership/master-data/snapshot");
        request.setServletPath("/v2/membership/master-data/snapshot");
        request.addHeader("X-TenantID", TENANT);
        assertNull(policy.approvedTenant(request));
        request.addHeader("Authorization", "Bearer abc");
        assertNull(policy.approvedTenant(request));
    }

    @Test void failedRefreshIsRetryableOnlyForEmergencyTenantAndWindow() {
        var policy = new MawaPayEmergencyBearerPolicy(true, TENANT, FUTURE);
        assertTrue(policy.protectRefreshFailure(request("POST", "/v2/refresh-token", TENANT), "aaa.bbb.ccc"));
        assertFalse(policy.protectRefreshFailure(request("POST", "/v2/refresh-token", "other-tenant"), "aaa.bbb.ccc"));
        assertFalse(policy.protectRefreshFailure(request("GET", "/v2/refresh-token", TENANT), "aaa.bbb.ccc"));
        assertFalse(policy.protectRefreshFailure(request("POST", "/v2/admin/users", TENANT), "aaa.bbb.ccc"));
        assertFalse(policy.protectRefreshFailure(request("POST", "/v2/refresh-token", TENANT), ""));
    }
}
