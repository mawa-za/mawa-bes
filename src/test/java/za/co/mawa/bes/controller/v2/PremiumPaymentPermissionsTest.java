package za.co.mawa.bes.controller.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.service.v2.MembershipPremiumPaymentService;
import za.co.mawa.bes.service.v2.PremiumPaymentEditService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PremiumPaymentPermissionsTest {
    @BeforeEach
    void clearSessionBeforeTest() {
        UserContext.clear();
    }

    @AfterEach
    void clearSessionAfterTest() {
        UserContext.clear();
    }

    @Test
    void deniedRoleCannotMutatePaymentsThroughAnyOnlineEndpoint() {
        var payments = mock(MembershipPremiumPaymentService.class);
        var edits = mock(PremiumPaymentEditService.class);
        var access = mock(UserAccessService.class);
        var controller = new PaymentBatchControllerV2(payments, edits, access);
        for (String action : new String[]{"create", "edit", "cancel", "transfer"}) {
            doThrow(new SecurityException("Denied"))
                    .when(access).assertPermission("premium-payment-" + action, "viewer");
        }
        assertThrows(SecurityException.class, () -> controller.createMembershipPremiumPayment(null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.captureManualMembershipPremiumReceipt(null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.requestPremiumPaymentEdit("batch", null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.requestPremiumPaymentDeletion("batch", null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.requestPremiumPaymentCancellation("batch", null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.transferManualPremiumPayment("batch", null, "viewer"));
        var sync = new OfflineSyncControllerV2(null, null, null, access);
        assertThrows(SecurityException.class, () -> sync.syncMembershipPremiumPayment(null, "viewer"));
        // A denied role must never reach a financial mutation service.
        verifyNoInteractions(payments, edits);
        verify(access, times(3)).assertPermission("premium-payment-create", "viewer");
        verify(access).assertPermission("premium-payment-edit", "viewer");
        verify(access, times(2)).assertPermission("premium-payment-cancel", "viewer");
        verify(access).assertPermission("premium-payment-transfer", "viewer");
    }

    @Test
    void permissionsReflectSelectedRoleIndependently() {
        var access = mock(UserAccessService.class);
        when(access.hasPermission("premium-payment-create", "collector")).thenReturn(true);
        var controller = new PaymentBatchControllerV2(null, null, access);
        var permissions = controller.permissions("collector");
        assertTrue(permissions.get("create"));
        assertFalse(permissions.get("edit"));
        assertFalse(permissions.get("cancel"));
        assertFalse(permissions.get("transfer"));
        verify(access).hasPermission("premium-payment-create", "collector");
        verify(access).hasPermission("premium-payment-edit", "collector");
        verify(access).hasPermission("premium-payment-cancel", "collector");
        verify(access).hasPermission("premium-payment-transfer", "collector");
    }
}
