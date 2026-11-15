package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import za.co.mawa.bes.configuration.context.TenantContext;

class TenantOutgoingEmailOutboxStoreTest {
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void refusesUnscopedPersistenceConnections() {
        EntityManager entityManager = mock(EntityManager.class);
        TenantOutgoingEmailOutboxStore store = new TenantOutgoingEmailOutboxStore(entityManager);
        assertThrows(IllegalStateException.class, () -> store.claimNext(5));
        assertThrows(IllegalStateException.class, () -> store.findInvoiceStatus("invoice1", "12345678-1234-1234-1234-123456789abc"));
        assertThrows(IllegalStateException.class, () -> store.findSourceStatus(
                "billing", "INV-123", "12345678-1234-1234-1234-123456789abc"));
        verifyNoInteractions(entityManager);
    }
    @Test void rejectsMalformedSourceStatusWithoutAccessingDatabase() {
        var em = mock(EntityManager.class);
        var store = new TenantOutgoingEmailOutboxStore(em);
        assertThrows(IllegalArgumentException.class, () -> store.findSourceStatus(
                "billing'; DROP TABLE outbound_email_message;--", "INV-123",
                "12345678-1234-1234-1234-123456789abc"));
        assertThrows(IllegalArgumentException.class, () -> store.findSourceStatus(
                "billing", "", "12345678-1234-1234-1234-123456789abc"));
        assertThrows(IllegalArgumentException.class, () -> store.findSourceStatus(
                "billing", "INV-123", "not-a-valid-uuid"));
        verifyNoInteractions(em);
    }

    @Test void retryBackoffIsBounded() {
        assertEquals(30, TenantOutgoingEmailOutboxStore.retryDelaySeconds(1));
        assertEquals(60, TenantOutgoingEmailOutboxStore.retryDelaySeconds(2));
        assertEquals(120, TenantOutgoingEmailOutboxStore.retryDelaySeconds(3));
        assertEquals(3600, TenantOutgoingEmailOutboxStore.retryDelaySeconds(30));
    }
}
