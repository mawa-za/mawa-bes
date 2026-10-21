package za.co.mawa.bes.xero;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import za.co.mawa.bes.configuration.context.UserContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XeroVatPolicyServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final XeroVatPolicyService service = new XeroVatPolicyService(jdbc);

    @AfterEach void clear() { UserContext.clear(); }

    @Test void cannotWriteWithoutExplicitPermission() {
        UserContext.setCurrentUserId("u1");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("u1"))).thenReturn(0);
        assertThrows(AccessDeniedException.class, () -> service.create(validRequest()));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void invalidVatConfigurationCannotBeWritten() {
        assertThrows(IllegalArgumentException.class, () -> XeroVatPolicyService.validate(
            new XeroVatPolicyService.PolicyRequest(true, null, "INCLUSIVE",
                new BigDecimal("15"), LocalDate.now().plusDays(1), null)));
        assertThrows(IllegalArgumentException.class, () -> XeroVatPolicyService.validate(
            new XeroVatPolicyService.PolicyRequest(false, null, "EXCLUSIVE",
                new BigDecimal("15"), LocalDate.now().plusDays(1), null)));
        assertThrows(IllegalArgumentException.class, () -> XeroVatPolicyService.validate(
            new XeroVatPolicyService.PolicyRequest(true, "123", "INCLUSIVE",
                new BigDecimal("15"), LocalDate.now().minusDays(1), null)));
    }

    @Test void blocksOverlappingPolicies() {
        UserContext.setCurrentUserId("u1");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("u1"))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(1);
        assertThrows(IllegalArgumentException.class, () -> service.create(validRequest()));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void validPolicyIsAppendOnlyAndNeverEnablesPosting() {
        UserContext.setCurrentUserId("u1");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("u1"))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);
        var result = service.create(validRequest());
        assertEquals(false, result.get("releaseAllowed"));
        verify(jdbc).update(contains("INSERT INTO xero_tenant_vat_policy"), any(Object[].class));
    }

    private XeroVatPolicyService.PolicyRequest validRequest() {
        return new XeroVatPolicyService.PolicyRequest(true, "4123456789", "INCLUSIVE",
                new BigDecimal("15.0000"), LocalDate.now().plusDays(1), null);
    }
}
