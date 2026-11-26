package za.co.mawa.bes.xero;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import za.co.mawa.bes.configuration.context.UserContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class XeroReconciliationPermissionTest {
    @Test void noExplicitGrantBlocksWorklist() {
        var jdbc = mock(JdbcTemplate.class);
        var service = new XeroReconciliationOperationsService(jdbc, mock(XeroInvoiceReconciliationService.class));
        try (var user = mockStatic(UserContext.class)) {
            user.when(UserContext::getCurrentUserId).thenReturn("user-1");
            when(jdbc.queryForObject(contains("xero-reconciliation-check"), eq(Integer.class), eq("user-1"))).thenReturn(0);
            assertFalse(service.canCheck());
            assertThrows(AccessDeniedException.class, service::pending);
            verify(jdbc, never()).queryForList(anyString());
        }
    }
    @Test void explicitGrantAllowsCapabilityAndUnauthenticatedDoesNot() {
        var jdbc = mock(JdbcTemplate.class);
        var service = new XeroReconciliationOperationsService(jdbc, mock(XeroInvoiceReconciliationService.class));
        try (var user = mockStatic(UserContext.class)) {
            user.when(UserContext::getCurrentUserId).thenReturn(null);
            assertFalse(service.canCheck());
            verifyNoInteractions(jdbc);
            user.when(UserContext::getCurrentUserId).thenReturn("user-1");
            when(jdbc.queryForObject(contains("xero-reconciliation-check"), eq(Integer.class), eq("user-1"))).thenReturn(1);
            assertTrue(service.canCheck());
        }
    }
}
