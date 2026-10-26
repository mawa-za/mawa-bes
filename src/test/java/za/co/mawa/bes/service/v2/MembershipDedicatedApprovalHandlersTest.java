package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MembershipDedicatedApprovalHandlersTest {
    @Test
    void deceasedHandlerDelegatesAllFinalDecisions() {
        MembershipChangeService service = mock(MembershipChangeService.class);
        var handler = new MembershipDependentDeceasedApprovalHandler(service);
        var request = new ApprovalRequestEntity();
        request.setReferenceId("change-1");
        assertEquals(ApprovalType.MEMBERSHIP_DEPENDENT_DECEASED, handler.supports());
        handler.onApproved(request, "manager");
        handler.onRejected(request, "manager");
        handler.onCancelled(request, "manager");
        verify(service).approved("change-1", "manager");
        verify(service).rejected("change-1", "manager", false);
        verify(service).rejected("change-1", "manager", true);
    }

    @Test
    void eligibilityHandlerDelegatesAllFinalDecisions() {
        MembershipChangeService service = mock(MembershipChangeService.class);
        var handler = new MembershipDependentClaimEligibilityApprovalHandler(service);
        var request = new ApprovalRequestEntity();
        request.setReferenceId("change-2");
        assertEquals(ApprovalType.MEMBERSHIP_DEPENDENT_CLAIM_ELIGIBILITY, handler.supports());
        handler.onApproved(request, "manager");
        handler.onRejected(request, "manager");
        handler.onCancelled(request, "manager");
        verify(service).approved("change-2", "manager");
        verify(service).rejected("change-2", "manager", false);
        verify(service).rejected("change-2", "manager", true);
    }
}
