package za.co.mawa.bes.controller.v2;

import org.junit.jupiter.api.Test;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.service.v2.MembershipPremiumPaymentService;
import za.co.mawa.bes.service.v2.PremiumPaymentEditService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PremiumPaymentPermissionsTest {
    @Test
    void deniedRoleCannotMutatePaymentsThroughAnyOnlineEndpoint() {
        var payments = mock(MembershipPremiumPaymentService.class);
        var edits = mock(PremiumPaymentEditService.class);
        var access = mock(UserAccessService.class);
        var controller = new PaymentBatchControllerV2(payments, edits, access);
        for (String action : new String[]{"create", "edit", "cancel", "transfer"}) {
            doThrow(new SecurityException("Denied"))
                    .when(access).assertWorkcentreAccess("premium-payment-" + action, "viewer");
        }
        assertThrows(SecurityException.class, () -> controller.createMembershipPremiumPayment(null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.captureManualMembershipPremiumReceipt(null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.requestPremiumPaymentEdit("batch", null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.requestPremiumPaymentDeletion("batch", null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.requestPremiumPaymentCancellation("batch", null, "viewer"));
        assertThrows(SecurityException.class, () -> controller.transferManualPremiumPayment("batch", null, "viewer"));
        var sync = new OfflineSyncControllerV2(null, null, null, access);
        assertThrows(SecurityException.class, () -> sync.syncMembershipPremiumPayment(null, "viewer"));
        verifyNoInteractions(payments, edits);
    }

    @Test
    void permissionsReflectSelectedRoleIndependently() {
        var access = mock(UserAccessService.class);
        when(access.hasWorkcentreAccess("premium-payment-create", "collector")).thenReturn(true);
        var controller = new PaymentBatchControllerV2(null, null, access);
        var permissions = controller.permissions("collector");
        assertTrue(permissions.get("create"));
        assertFalse(permissions.get("edit"));
        assertFalse(permissions.get("cancel"));
        assertFalse(permissions.get("transfer"));
    }
}
