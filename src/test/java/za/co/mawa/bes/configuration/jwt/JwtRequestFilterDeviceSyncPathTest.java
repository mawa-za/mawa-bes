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
}
