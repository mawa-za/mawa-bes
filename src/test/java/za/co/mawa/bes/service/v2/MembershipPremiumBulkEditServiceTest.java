package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.MembershipPremiumBulkEditRequest;
import za.co.mawa.bes.dto.v2.MembershipPremiumEditRequest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MembershipPremiumBulkEditServiceTest {
    private final MembershipPremiumEditService edits = mock(MembershipPremiumEditService.class);
    private final MembershipPremiumBulkEditService service = new MembershipPremiumBulkEditService(edits);

    private MembershipPremiumBulkEditRequest request(String... premiumIds) {
        MembershipPremiumBulkEditRequest r = new MembershipPremiumBulkEditRequest();
        r.setReason("Annual adjustment approved by finance");
        List<MembershipPremiumBulkEditRequest.Line> rows = new ArrayList<>();
        for (String id : premiumIds) {
            var line = new MembershipPremiumBulkEditRequest.Line();
            line.setMembershipId("member-" + id);
            line.setPremiumId(id);
            line.setAmountCents(12000L);
            line.setExpectedAmountCents(10000L);
            line.setExpectedPaidAmountCents(2000L);
            rows.add(line);
        }
        r.setLines(rows);
        return r;
    }

    @Test
    void submitsEachLineWithSharedBatchIdAndItsOwnApproval() {
        when(edits.requestBatchEdit(anyString(), anyString(), any(MembershipPremiumEditRequest.class),
                eq("requester"), anyString(), anyInt(), eq(2)))
                .thenAnswer(invocation -> ApprovalRequestResponse.builder()
                        .id("approval-" + invocation.getArgument(1, String.class)).build());
        var result = service.submit(request("premium-a", "premium-b"), "requester");
        assertEquals(2, result.submittedCount());
        assertEquals(2, result.approvals().size());
        assertEquals("approval-premium-a", result.approvals().get(0).approvalRequestId());
        assertEquals("approval-premium-b", result.approvals().get(1).approvalRequestId());
        var batch = result.batchId();
        assertNotNull(batch);
        verify(edits).requestBatchEdit(eq("member-premium-a"), eq("premium-a"),
                argThat(line -> line.getAmountCents() == 12000L
                        && line.getExpectedAmountCents() == 10000L
                        && line.getExpectedPaidAmountCents() == 2000L
                        && "Annual adjustment approved by finance".equals(line.getReason())),
                eq("requester"), eq(batch), eq(1), eq(2));
        verify(edits).requestBatchEdit(eq("member-premium-b"), eq("premium-b"),
                any(), eq("requester"), eq(batch), eq(2), eq(2));
    }

    @Test
    void rejectsDuplicateRowsBeforeSubmittingAnyApproval() {
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(request("premium-a", "premium-a"), "requester"));
        verifyNoInteractions(edits);
    }

    @Test
    void rejectsMissingOriginalAmountAndMoreThanOneHundredPeriods() {
        var missing = request("p1");
        missing.getLines().get(0).setExpectedAmountCents(null);
        assertThrows(IllegalArgumentException.class, () -> service.submit(missing, "requester"));
        var ids = new String[101];
        for (int i = 0; i < ids.length; i++) ids[i] = "p" + i;
        assertThrows(IllegalArgumentException.class, () -> service.submit(request(ids), "requester"));
        verifyNoInteractions(edits);
    }

    @Test
    void invalidReasonAndNoSelectionCannotSubmit() {
        var noLines = request();
        assertThrows(IllegalArgumentException.class, () -> service.submit(noLines, "requester"));
        var blankReason = request("p1");
        blankReason.setReason(" ");
        assertThrows(IllegalArgumentException.class, () -> service.submit(blankReason, "requester"));
        verifyNoInteractions(edits);
    }

    @Test
    void isTransactionalSoLaterFailureRollsBackEarlierApprovals() throws Exception {
        Method method = MembershipPremiumBulkEditService.class.getMethod(
                "submit", MembershipPremiumBulkEditRequest.class, String.class);
        assertNotNull(method.getAnnotation(Transactional.class));
        when(edits.requestBatchEdit(anyString(), anyString(), any(), anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    if (invocation.getArgument(1, String.class).equals("bad"))
                        throw new IllegalStateException("Premium already pending approval");
                    return ApprovalRequestResponse.builder().id("first").build();
                });
        assertThrows(IllegalStateException.class,
                () -> service.submit(request("ok", "bad"), "requester"));
        verify(edits, times(2)).requestBatchEdit(anyString(), anyString(), any(), anyString(), anyString(), anyInt(), eq(2));
    }
}
