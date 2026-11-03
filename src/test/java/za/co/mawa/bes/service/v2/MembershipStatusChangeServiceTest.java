package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.dto.v2.MembershipStatusChangeRequest;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MembershipStatusChangeServiceTest {
    private final MembershipRepository repository = mock(MembershipRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ApprovalService approvals = mock(ApprovalService.class);
    private final MembershipStatusChangeService service =
            new MembershipStatusChangeService(repository, approvals, jdbc, new ObjectMapper());
    private MembershipEntity membership;

    @BeforeEach
    void setUp() {
        membership = new MembershipEntity();
        membership.setId("member-1");
        membership.setStatus("LAPSED");
        when(repository.findByIdForUpdate("member-1")).thenReturn(Optional.of(membership));
        when(jdbc.queryForList(anyString(), eq("action-1"))).thenReturn(List.of(Map.of(
                "membership_id", "member-1", "status", "PENDING_APPROVAL",
                "previous_status", "LAPSED", "requested_status", "ACTIVE")));
    }

    @Test
    void reactivationPreservesUnexpiredWaitingPeriod() {
        LocalDate eligibility = LocalDate.now().plusMonths(2);
        membership.setBenefitEligibleFrom(eligibility);
        service.complete("action-1", true, "approver", "APPROVED");
        assertEquals("WAITING_PERIOD", membership.getStatus());
        assertEquals(eligibility, membership.getBenefitEligibleFrom());
        verify(repository).save(membership);
    }

    @Test
    void reactivationIsActiveOnEligibilityDate() {
        membership.setBenefitEligibleFrom(LocalDate.now());
        service.complete("action-1", true, "approver", "APPROVED");
        assertEquals("ACTIVE", membership.getStatus());
    }

    @Test
    void rejectionDoesNotChangeMembershipOrDates() {
        LocalDate eligibility = LocalDate.now().plusDays(1);
        membership.setBenefitEligibleFrom(eligibility);
        service.complete("action-1", false, "approver", "REJECTED");
        assertEquals("LAPSED", membership.getStatus());
        assertEquals(eligibility, membership.getBenefitEligibleFrom());
        verify(repository, never()).save(any());
    }

    @Test
    void staleApprovalCannotOverwriteChangedMembership() {
        membership.setStatus("CANCELLED");
        assertThrows(IllegalStateException.class,
                () -> service.complete("action-1", true, "approver", "APPROVED"));
        verify(repository, never()).save(any());
    }

    @Test
    void submissionRequiresManualApprovalAndUsesAuthenticatedActor() {
        membership.setMembershipNo("M1");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("member-1"))).thenReturn(0);
        var request = new MembershipStatusChangeRequest();
        request.setReason("Reinstate cover");
        request.setRequestedBy("spoofed-user");
        when(approvals.submitForMandatoryApproval(any())).thenThrow(new IllegalStateException("No manual workflow"));
        assertThrows(IllegalStateException.class,
                () -> service.request("member-1", "REACTIVATE", request, "authenticated-user"));
        verify(approvals).submitForMandatoryApproval(argThat(submission ->
                "authenticated-user".equals(submission.getRequesterId())));
        verify(approvals, never()).submitForApproval(any(ApprovalSubmitRequest.class));
        verify(repository, never()).save(any());
    }
}
