package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.entity.v2.MembershipChangeRequestEntity;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.enums.MembershipChangeStatus;
import za.co.mawa.bes.enums.MembershipChangeType;
import za.co.mawa.bes.enums.MembershipDependentStatus;
import za.co.mawa.bes.repository.v2.MembershipChangeAuditRepository;
import za.co.mawa.bes.repository.v2.MembershipChangeRequestRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipDependentRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipDependentApprovalActionsTest {
    @Mock MembershipDependentRepository dependents;
    @Mock MembershipClaimRepository claims;
    @Mock MembershipRepository memberships;
    @Mock MembershipChangeRequestRepository changes;
    @Mock MembershipChangeAuditRepository audit;
    @Mock MembershipUpdateHandlerRegistry membershipUpdates;
    @Spy ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @InjectMocks MembershipChangeService service;

    @Test
    void capacityCountsEveryDeceasedDependentEvenWithoutAnyClaim() {
        when(dependents.countByMembershipIdAndStatusIn(eq("m1"), anyCollection())).thenReturn(3L);

        Long count = ReflectionTestUtils.invokeMethod(service, "dependentCapacityCount", "m1");

        assertEquals(3L, count);
        verify(dependents).countByMembershipIdAndStatusIn(
                "m1", Set.of(MembershipDependentStatus.ACTIVE, MembershipDependentStatus.DECEASED));
        verifyNoInteractions(claims);
    }

    @Test
    void approvedHistoricDeathChangesOnlyDependentRelationship() {
        var effectiveDate = LocalDate.of(2026, 8, 15);
        var dependent = activeDependent(LocalDate.of(2025, 1, 1));
        when(dependents.findByIdAndMembershipId("d1", "m1")).thenReturn(Optional.of(dependent));
        when(dependents.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var request = request(MembershipChangeType.MARK_DEPENDENT_DECEASED, effectiveDate);

        ReflectionTestUtils.invokeMethod(service, "applyDependentChange", request, "approver");

        assertEquals(MembershipDependentStatus.DECEASED, dependent.getStatus());
        assertFalse(dependent.getActive());
        assertEquals(effectiveDate, dependent.getDeceasedDate());
        assertEquals(effectiveDate, dependent.getEffectiveTo());
        assertEquals(MembershipChangeStatus.APPLIED, request.getStatus());
        verifyNoInteractions(memberships);
        verifyNoInteractions(claims);
    }

    @Test
    void approvedEligibilityCanUseExplicitEarlierDateWithoutChangingMembershipDates() {
        var dependent = activeDependent(LocalDate.of(2026, 12, 1));
        var newDate = LocalDate.of(2026, 10, 1);
        var membership = new MembershipEntity();
        membership.setId("m1");
        membership.setStartDate(LocalDate.of(2025, 1, 1));
        when(memberships.findById("m1")).thenReturn(Optional.of(membership));
        when(dependents.findByIdAndMembershipId("d1", "m1")).thenReturn(Optional.of(dependent));
        when(dependents.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var request = request(MembershipChangeType.OVERRIDE_DEPENDENT_WAITING_PERIOD, newDate);
        request.setOldDependentEffectiveFrom(LocalDate.of(2026, 12, 1));

        ReflectionTestUtils.invokeMethod(service, "applyDependentChange", request, "approver");

        assertEquals(newDate, dependent.getEffectiveFrom());
        assertEquals(MembershipDependentStatus.ACTIVE, dependent.getStatus());
        assertEquals(LocalDate.of(2025, 1, 1), membership.getStartDate());
        assertEquals(MembershipChangeStatus.APPLIED, request.getStatus());
        verify(membershipUpdates).handleUpdate("m1");
    }

    private MembershipDependentEntity activeDependent(LocalDate from) {
        return MembershipDependentEntity.builder().id("d1").membershipId("m1")
                .dependentPartnerId("p1").effectiveFrom(from)
                .status(MembershipDependentStatus.ACTIVE).active(true).build();
    }

    private MembershipChangeRequestEntity request(MembershipChangeType type, LocalDate date) {
        return MembershipChangeRequestEntity.builder()
                .id("r1").membershipId("m1").changeType(type)
                .status(MembershipChangeStatus.PENDING_APPROVAL)
                .oldDependentId("d1").oldDependentPartnerId("p1")
                .oldDependentEffectiveFrom(LocalDate.of(2026, 12, 1))
                .effectiveDate(date).reason("Evidence reviewed")
                .requestedBy("requester").build();
    }
}
