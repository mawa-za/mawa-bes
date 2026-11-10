package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.repository.RoleRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock RoleRepository roles;
    @Mock UserAccessService access;

    @Test
    void onlyProtectedAdministratorsMayReplacePermissionGrants() {
        when(access.isProtectedAdministrator()).thenReturn(false);
        RolePermissionService service = new RolePermissionService(jdbc, roles, access);
        assertThrows(SecurityException.class,
                () -> service.replace("CASHIER", List.of("premium-payment-create")));
        verifyNoInteractions(jdbc, roles);
    }

    @Test
    void unknownPermissionsAreRejectedWithoutSaving() {
        when(access.isProtectedAdministrator()).thenReturn(true);
        when(roles.existsById("CASHIER")).thenReturn(true);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of("premium-payment-create"));
        RolePermissionService service = new RolePermissionService(jdbc, roles, access);
        assertThrows(IllegalArgumentException.class,
                () -> service.replace("CASHIER", List.of("premium-payment-cancel")));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void savedPermissionsAreAuditedAndIndependentOfWorkcentres() {
        UserContext.clear();
        UserContext.setCurrentUserId("admin-1");
        try {
            when(access.isProtectedAdministrator()).thenReturn(true);
            when(roles.existsById("CASHIER")).thenReturn(true);
            when(jdbc.queryForList("SELECT code FROM permission_catalog WHERE active = TRUE", String.class))
                    .thenReturn(List.of("premium-payment-create", "premium-payment-cancel"));
            when(jdbc.queryForList(startsWith("SELECT permission_code FROM role_permission"),
                    eq(String.class), eq("CASHIER")))
                    .thenReturn(List.of("premium-payment-create"),
                            List.of("premium-payment-create", "premium-payment-cancel"));
            RolePermissionService service = new RolePermissionService(jdbc, roles, access);
            assertEquals(List.of("premium-payment-create", "premium-payment-cancel"),
                    service.replace("CASHIER", List.of("premium-payment-create", "premium-payment-cancel")));
            verify(jdbc).update(startsWith("INSERT INTO role_permission("),
                    eq("CASHIER"), eq("premium-payment-cancel"));
            verify(jdbc).update(startsWith("INSERT INTO role_permission_audit("),
                    eq("CASHIER"), eq("premium-payment-cancel"), eq("GRANT"), eq("admin-1"));
        } finally {
            UserContext.clear();
        }
    }
}
