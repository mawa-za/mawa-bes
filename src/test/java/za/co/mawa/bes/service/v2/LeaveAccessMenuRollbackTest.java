package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaveAccessMenuRollbackTest {

    @Test
    void expectedMissingEmploymentDoesNotMarkJoinedTransactionRollbackOnly() throws Exception {
        TransactionAttribute attribute = new AnnotationTransactionAttributeSource()
                .getTransactionAttribute(
                        LeaveAccessService.class.getMethod("currentEmployment", LocalDate.class),
                        LeaveAccessService.class);
        assertNotNull(attribute);
        assertFalse(attribute.rollbackOn(new NoSuchElementException("No active employment")));
        assertFalse(attribute.rollbackOn(new SecurityException("No employee partner")));
        assertTrue(attribute.rollbackOn(new IllegalStateException("Unexpected failure")));
    }

    @Test
    void nonEmployeeCanOpenLeaveManagementWithoutApproverAccess() {
        LeaveAccessService access = mock(LeaveAccessService.class);
        when(access.currentEmployment(any(LocalDate.class)))
                .thenThrow(new NoSuchElementException("No active employment record"));

        Map<String, Object> response = leaveService(access).access();

        assertEquals(false, response.get("hasEmployment"));
        assertEquals(false, response.get("leaveApprover"));
        assertFalse(response.containsKey("employmentId"));
    }

    @Test
    void unlinkedUserWithLeaveApprovalAssignmentCanOpenTeamLeave() {
        LeaveAccessService access = mock(LeaveAccessService.class);
        when(access.currentEmployment(any(LocalDate.class)))
                .thenThrow(new SecurityException("No linked employee partner"));
        when(access.isLeaveApprover()).thenReturn(true);

        Map<String, Object> response = leaveService(access).access();

        assertEquals(false, response.get("hasEmployment"));
        assertEquals(true, response.get("leaveApprover"));
        assertFalse(response.containsKey("employmentId"));
    }

    private LeaveRequestV2Service leaveService(LeaveAccessService access) {
        // Other collaborators are not used by access() when there is no employment record.
        return new LeaveRequestV2Service(
                null, null, null, null, null, null, null, null, null, null, access, null);
    }
}
