package za.co.mawa.bes.service.v2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.UserAccessService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BankTransactionTestResetServiceTest {
    private final MockEnvironment environment = new MockEnvironment();
    private final EntityManager entityManager = mock(EntityManager.class);
    private final UserAccessService access = mock(UserAccessService.class);
    private final Map<String, Long> rows = new HashMap<>();
    private final BankTransactionTestResetService service =
            new BankTransactionTestResetService(entityManager, environment, access);

    @BeforeEach
    void setup() {
        TenantContext.setCurrentTenant("qa-bank-tenant");
        UserContext.setCurrentUserId("admin-1");
        environment.setProperty("MAWA_ENVIRONMENT", "hotfix");
        environment.setProperty("mawa.bank-test-reset.enabled", "true");
        environment.setProperty("mawa.bank-test-reset.allowed-tenants", "qa-bank-tenant");
        when(access.isProtectedAdministrator()).thenReturn(true);
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            Query q = mock(Query.class);
            when(q.getSingleResult()).thenReturn(rows.getOrDefault(sql, 0L));
            if (sql.startsWith("DELETE FROM bank_transaction")) when(q.executeUpdate()).thenReturn(3);
            else if (sql.startsWith("DELETE FROM bank_statement_import")) when(q.executeUpdate()).thenReturn(2);
            else if (sql.startsWith("UPDATE bank_statement_file")) when(q.executeUpdate()).thenReturn(1);
            return q;
        });
        rows.put("SELECT COUNT(*) FROM bank_transaction", 3L);
        rows.put("SELECT COUNT(*) FROM bank_statement_import", 2L);
        rows.put("SELECT COUNT(*) FROM bank_statement_file WHERE storage_path IS NOT NULL AND status <> 'STORED'", 1L);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    void previewShowsSafeTenantSpecificCounts() {
        var p = service.preview();
        assertEquals("qa-bank-tenant", p.tenantId());
        assertEquals(3, p.transactions());
        assertEquals(2, p.importEnvelopes());
        assertEquals(1, p.filesToRearm());
        assertTrue(p.canReset());
    }

    @Test
    void neverRunsInProductionEvenWhenFlagIsTrue() {
        environment.setProperty("MAWA_ENVIRONMENT", "prod");
        assertThrows(ResponseStatusException.class, service::preview);
        verifyNoInteractions(entityManager);
    }

    @Test
    void cloudRunProductionServiceNameAlsoBlocks() {
        environment.setProperty("K_SERVICE", "mawa-bes-v2-prod");
        assertThrows(ResponseStatusException.class, service::preview);
        verifyNoInteractions(entityManager);
    }

    @Test
    void cloudRunServiceMustBeExplicitlyAllowlisted() {
        environment.setProperty("K_SERVICE", "mawa-bes-hotfix");
        assertThrows(ResponseStatusException.class, service::preview);
        environment.setProperty("mawa.bank-test-reset.allowed-services", "mawa-bes-hotfix");
        assertTrue(service.preview().canReset());
    }

    @Test
    void missingTenantAllowlistBlocks() {
        environment.setProperty("mawa.bank-test-reset.allowed-tenants", "other-tenant");
        assertThrows(ResponseStatusException.class, service::preview);
        verifyNoInteractions(entityManager);
    }

    @Test
    void disabledFeatureBlocksEvenOnHotfix() {
        environment.setProperty("mawa.bank-test-reset.enabled", "false");
        assertThrows(ResponseStatusException.class, service::preview);
        verifyNoInteractions(entityManager);
    }

    @Test
    void unprotectedUserCannotReset() {
        when(access.isProtectedAdministrator()).thenReturn(false);
        assertThrows(ResponseStatusException.class, service::preview);
        verifyNoInteractions(entityManager);
    }

    @Test
    void anyAllocationEvenReleasedBlocksEntireReset() {
        rows.put("SELECT COUNT(*) FROM bank_transaction_allocation", 1L);
        var p = service.preview();
        assertFalse(p.canReset());
        assertThrows(ResponseStatusException.class, () -> service.reset(
                new BankTransactionTestResetService.ResetRequest("qa-bank-tenant",
                        "RESET BANK HISTORY: qa-bank-tenant", 3, 2, 1)));
        verify(entityManager, never()).createNativeQuery(startsWith("DELETE "));
    }

    @Test
    void postedPaymentOrCashupLinksBlockReset() {
        rows.put("SELECT COUNT(*) FROM payment_batch WHERE bank_deposit_id IS NOT NULL", 1L);
        assertFalse(service.preview().canReset());
        rows.remove("SELECT COUNT(*) FROM payment_batch WHERE bank_deposit_id IS NOT NULL");
        rows.put("SELECT COUNT(*) FROM cashup_deposit WHERE bank_deposit_id IS NOT NULL", 1L);
        assertFalse(service.preview().canReset());
    }

    @Test
    void concurrentProcessingBlocksReset() {
        rows.put("SELECT COUNT(*) FROM bank_statement_file WHERE status = 'PROCESSING'", 1L);
        assertFalse(service.preview().canReset());
    }

    @Test
    void stalePreviewAndWrongTypedConfirmationAreRejected() {
        assertThrows(ResponseStatusException.class, () -> service.reset(
                new BankTransactionTestResetService.ResetRequest("qa-bank-tenant",
                        "RESET BANK HISTORY: qa-bank-tenant", 999, 2, 1)));
        assertThrows(ResponseStatusException.class, () -> service.reset(
                new BankTransactionTestResetService.ResetRequest("qa-bank-tenant",
                        "RESET BANK HISTORY", 3, 2, 1)));
        verify(entityManager, never()).createNativeQuery(startsWith("DELETE "));
    }

    @Test
    void resetDeletesOnlyUnlinkedBankRowsAndPreservesArchivedFilesAndAudit() {
        var result = service.reset(new BankTransactionTestResetService.ResetRequest(
                "qa-bank-tenant", "RESET BANK HISTORY: qa-bank-tenant", 3, 2, 1));
        assertEquals("RESET_COMPLETE", result.status());
        assertEquals(3, result.transactionsRemoved());
        assertEquals(2, result.importsRemoved());
        assertEquals(1, result.filesRearmed());
        verify(entityManager).createNativeQuery("DELETE FROM bank_transaction");
        verify(entityManager).createNativeQuery("DELETE FROM bank_statement_import");
        verify(entityManager, never()).createNativeQuery(contains("DELETE FROM bank_statement_file"));
        verify(entityManager, never()).createNativeQuery(contains("DELETE FROM bank_transaction_allocation"));
        verify(access).audit(eq("BANK_TEST_HISTORY_RESET"), eq("BANK_TRANSACTION_HISTORY"),
                eq("qa-bank-tenant"), anyString(), contains("Deleted transactions=3"));
    }
}
