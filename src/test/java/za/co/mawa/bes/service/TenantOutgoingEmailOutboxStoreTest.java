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
        assertThrows(IllegalStateException.class, () -> store.findInvoiceStatus("invoice1", "req1"));
        verifyNoInteractions(entityManager);
    }
    @Test void retryBackoffIsBounded() {
        assertEquals(30, TenantOutgoingEmailOutboxStore.retryDelaySeconds(1));
        assertEquals(60, TenantOutgoingEmailOutboxStore.retryDelaySeconds(2));
        assertEquals(120, TenantOutgoingEmailOutboxStore.retryDelaySeconds(3));
        assertEquals(3600, TenantOutgoingEmailOutboxStore.retryDelaySeconds(30));
    }
}
