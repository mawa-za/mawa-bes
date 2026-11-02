package za.co.mawa.bes.configuration.jwt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtRequestFilterDeviceSyncPathTest {
    @Test void permitsReceiptCancellationStatus() {
        assertTrue(JwtRequestFilter.isDeviceSyncPath("/v2/payment-batches/batch-123/cancellation-status"));
    }

    @Test void permitsSingularNumberAllocationEndpoints() {
        assertTrue(JwtRequestFilter.isDeviceSyncPath("/v2/number-allocation/allocate"));
        assertTrue(JwtRequestFilter.isDeviceSyncPath("/v2/number-allocation/active"));
    }

    @Test void deniesOtherPaymentBatchActionsToDeviceIdentity() {
        assertFalse(JwtRequestFilter.isDeviceSyncPath("/v2/payment-batches/batch-123/deletion-request"));
        assertFalse(JwtRequestFilter.isDeviceSyncPath("/v2/payment-batches/batch-123/cancellation-request"));
        assertFalse(JwtRequestFilter.isDeviceSyncPath("/v2/payment-batches/batch-123/edit-request"));
    }

    @Test void permitsMasterDataReadButNotMasterDataWrites() {
        assertTrue(JwtRequestFilter.isDeviceSyncRequest("GET", "/v2/pay-app/members"));
        assertTrue(JwtRequestFilter.isDeviceSyncRequest("GET", "/v2/pay-app/master-data/members"));
        assertTrue(JwtRequestFilter.isDeviceSyncRequest("GET", "/v2/group-society/master-data"));
        assertTrue(JwtRequestFilter.isDeviceSyncRequest("GET", "/v2/field/option"));
        assertFalse(JwtRequestFilter.isDeviceSyncRequest("POST", "/v2/group-society/master-data"));
        assertFalse(JwtRequestFilter.isDeviceSyncRequest("POST", "/v2/pay-app/master-data/members"));
        assertFalse(JwtRequestFilter.isDeviceSyncRequest("POST", "/v2/pay-app/members"));
        assertFalse(JwtRequestFilter.isDeviceSyncRequest("GET", "/v2/pay-app/manual-actions"));
        assertFalse(JwtRequestFilter.isDeviceSyncRequest("GET", "/v2/pay-app/admin"));
    }

    @Test void manualActionsStillRequireUserAuthentication() {
        assertFalse(JwtRequestFilter.isDeviceSyncRequest("POST", "/v2/pay-app/manual-actions"));
    }
}
