package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.MembershipPremiumEditRequest;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.PremiumStatus;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MembershipPremiumBatchApprovalTest {
    private final MembershipPremiumRepository premiums = mock(MembershipPremiumRepository.class);
    private final MembershipPremiumService reconcile = mock(MembershipPremiumService.class);
    private final MembershipService memberships = mock(MembershipService.class);
    private final MembershipActionGuardService guard = mock(MembershipActionGuardService.class);
    private final ApprovalService approvals = mock(ApprovalService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final MembershipPremiumEditService service = new MembershipPremiumEditService(
            premiums, reconcile, memberships, guard, approvals, jdbc, mapper);

    private MembershipPremiumEditRequest edit(long original, long proposed, long paid) {
        var request = new MembershipPremiumEditRequest();
        request.setAmountCents(proposed);
        request.setExpectedAmountCents(original);
        request.setExpectedPaidAmountCents(paid);
        request.setReason("Premium adjustment");
        return request;
    }

    private void setup(long amount, long paid) {
        var member = new MembershipEntity();
        member.setId("member-1");
        member.setMembershipNo("M001");
        when(memberships.resolveMembership("member-1")).thenReturn(member);
        when(memberships.membershipIdentifiers("member-1")).thenReturn(List.of("member-1"));
        var premium = new MembershipPremiumEntity();
        premium.setId("premium-1");
        premium.setMembershipId("member-1");
        premium.setPeriodYYYYMM("202610");
        premium.setAmountCents(amount);
        premium.setPaidAmountCents(paid);
        premium.setBalanceCents(amount - paid);
        premium.setStatus(paid > 0 ? PremiumStatus.PARTIALLY_PAID : PremiumStatus.UNPAID);
        when(premiums.findForPremiumEdit("premium-1")).thenReturn(Optional.of(premium));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("premium-1"))).thenReturn(0);
        when(approvals.submitForMandatoryApproval(any())).thenReturn(
                ApprovalRequestResponse.builder().id("approval-1").build());
    }

    @Test
    void bulkRequestUsesManualApprovalAndIncludesAuditCorrelation() throws Exception {
        setup(10000, 2000);
        var response = service.requestBatchEdit("member-1", "premium-1", edit(10000, 12000, 2000),
                "requester", "batch-123", 1, 3);
        assertEquals("approval-1", response.getId());
        ArgumentCaptor<ApprovalSubmitRequest> capture = ArgumentCaptor.forClass(ApprovalSubmitRequest.class);
        verify(approvals).submitForMandatoryApproval(capture.capture());
        verify(approvals, never()).submitForApproval(any());
        var approval = capture.getValue();
        assertEquals(ApprovalType.MEMBERSHIP_PREMIUM_EDIT, approval.getApprovalType());
        assertEquals("requester", approval.getRequesterId());
        var payload = mapper.readTree(approval.getPayloadJson());
        assertEquals("batch-123", payload.path("bulkRequestId").asText());
        assertEquals(1, payload.path("bulkLine").asInt());
        assertEquals(3, payload.path("bulkTotal").asInt());
        assertEquals(12000L, payload.path("proposedValues").path("amountCents").asLong());
        verify(premiums, never()).save(any());
    }

    @Test
    void staleReviewAndLessThanPaidNeverCreateApproval() {
        setup(10000, 8000);
        assertThrows(IllegalStateException.class, () -> service.requestBatchEdit(
                "member-1", "premium-1", edit(9000, 11000, 8000), "requester", "batch", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> service.requestBatchEdit(
                "member-1", "premium-1", edit(10000, 7000, 8000), "requester", "batch", 1, 1));
        verifyNoInteractions(approvals);
    }
}
