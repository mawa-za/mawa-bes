package za.co.mawa.bes.controller.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.MembershipPremiumBulkEditRequest;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.service.v2.MembershipPremiumBulkEditService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MembershipPremiumBulkEditControllerTest {
    @AfterEach
    void cleanup() { UserContext.clear(); }

    @Test
    void anUnauthorizedRoleCannotSubmitEvenWhenItHasWorkcenterNavigation() {
        var access = mock(UserAccessService.class);
        var service = mock(MembershipPremiumBulkEditService.class);
        doThrow(new SecurityException("Denied"))
                .when(access).assertPermission("premium-payment-edit", "viewer");
        var controller = new MembershipPremiumBulkEditControllerV2(service, access);
        assertThrows(SecurityException.class, () -> controller.submit(
                new MembershipPremiumBulkEditRequest(), () -> "requester", "viewer"));
        verifyNoInteractions(service);
        verify(access).assertPermission("premium-payment-edit", "viewer");
    }

    @Test
    void workcentreAloneIsInsufficientAndActionAloneIsInsufficient() {
        var access = mock(UserAccessService.class);
        var service = mock(MembershipPremiumBulkEditService.class);
        var controller = new MembershipPremiumBulkEditControllerV2(service, access);
        when(access.hasWorkcentreAccess("bulk-membership-premium-edits", "editor")).thenReturn(false);
        assertThrows(SecurityException.class, () -> controller.submit(
                new MembershipPremiumBulkEditRequest(), () -> "actor", "editor"));
        verifyNoInteractions(service);
        var permissions = controller.access("editor");
        org.junit.jupiter.api.Assertions.assertFalse(permissions.get("workcenter"));
    }

    @Test
    void permitsOnlyAnAuthenticatedActorAfterRolePermissionCheck() {
        var access = mock(UserAccessService.class);
        var service = mock(MembershipPremiumBulkEditService.class);
        var controller = new MembershipPremiumBulkEditControllerV2(service, access);
        when(access.hasWorkcentreAccess("bulk-membership-premium-edits", "editor")).thenReturn(true);
        assertThrows(SecurityException.class, () -> controller.submit(
                new MembershipPremiumBulkEditRequest(), null, "editor"));
        verifyNoInteractions(service);
    }
}
