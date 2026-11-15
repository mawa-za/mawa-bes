package za.co.mawa.bes.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import za.co.mawa.bes.configuration.context.TenantContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;

class OutgoingGatewaySubmissionServiceTest {
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void statusDelegatesWithAuthenticatedCallerAsSource() {
        var queue = mock(QueuedOutgoingEmailService.class);
        var service = new OutgoingGatewaySubmissionService(mock(EntityManager.class), queue);
        TenantContext.setCurrentTenant("qa_tenant");
        var request = new OutgoingGatewaySubmissionService.StatusRequest(
                "BILL-100", "12345678-1234-1234-1234-123456789abc");
        when(queue.sourceStatus("billing", request.reference(), request.requestId()))
                .thenReturn(Optional.empty());
        assertTrue(service.status("billing", request).isEmpty());
        verify(queue).sourceStatus("billing", "BILL-100", request.requestId());
        verifyNoMoreInteractions(queue);
    }

    @Test void deniesStatusWithoutTenantAndInvalidSource() {
        var queue = mock(QueuedOutgoingEmailService.class);
        var service = new OutgoingGatewaySubmissionService(mock(EntityManager.class), queue);
        var request = new OutgoingGatewaySubmissionService.StatusRequest(
                "BILL-100", "12345678-1234-1234-1234-123456789abc");
        assertThrows(SecurityException.class, () -> service.status("billing", request));
        TenantContext.setCurrentTenant("qa_tenant");
        assertThrows(SecurityException.class, () -> service.status("invoice", request));
        assertThrows(IllegalArgumentException.class, () -> service.status("billing",
                new OutgoingGatewaySubmissionService.StatusRequest("bad <html>", request.requestId())));
        verifyNoInteractions(queue);
    }
}
