package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.utils.TransactionType;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.PartnerService;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipServiceIdempotencyTest {

    private MembershipRepository membershipRepository;
    private MembershipService service;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUserId("actor-1");
        UserContext.setCurrentUserPartner("actor-partner");
        membershipRepository = mock(MembershipRepository.class);
        service = new MembershipService(membershipRepository);
        service.partnerRepository = mock(PartnerRepository.class);
        service.numberAllocationService = mock(NumberAllocationService.class);
        service.membershipPlanService = mock(MembershipPlanService.class);
        service.membershipChangeService = mock(MembershipChangeService.class);
        service.partnerService = mock(PartnerService.class);
        service.approvalService = mock(ApprovalService.class);
        service.objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void returnsExistingMembershipForExplicitNumberRetryOnSamePlan() {
        MembershipEntity request = membership("PARTNER-1", "PLAN-1", "MEM-1001");
        MembershipEntity existing = membership("PARTNER-1", "PLAN-1", "MEM-1001");
        existing.setId("SERVER-MEMBERSHIP-1");

        when(service.partnerRepository.existsById("PARTNER-1")).thenReturn(true);
        when(membershipRepository.findByMembershipNo("MEM-1001"))
                .thenReturn(Optional.of(existing));

        assertSame(existing, service.createMembership(request));
        verify(membershipRepository, never()).save(request);
    }

    @Test
    void rejectsExplicitNumberCollisionWithDifferentPlan() {
        MembershipEntity request = membership("PARTNER-1", "PLAN-2", "MEM-1001");
        MembershipEntity existing = membership("PARTNER-1", "PLAN-1", "MEM-1001");

        when(service.partnerRepository.existsById("PARTNER-1")).thenReturn(true);
        when(membershipRepository.findByMembershipNo("MEM-1001"))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.createMembership(request));
        verify(membershipRepository, never()).save(request);
    }

    @Test
    void blankNumberForExistingMemberCreatesPendingApprovalRatherThanRejectingSync() {
        MembershipEntity request = membership("PARTNER-1", "PLAN-2", "");

        when(service.partnerRepository.existsById("PARTNER-1")).thenReturn(true);
        when(membershipRepository.countByMemberId("PARTNER-1")).thenReturn(1L);
        when(service.numberAllocationService.allocateNumber(TransactionType.MEMBERSHIP))
                .thenReturn("MEM-2002");
        when(service.membershipPlanService.getPlanById("PLAN-2"))
                .thenReturn(Optional.of(MembershipPlanEntity.builder()
                        .premiumCents(15_000L).waitingPeriodMonths(3).build()));
        when(membershipRepository.save(any(MembershipEntity.class))).thenAnswer(invocation -> {
            MembershipEntity saved = invocation.getArgument(0);
            saved.setId("SERVER-MEMBERSHIP-2");
            return saved;
        });
        when(service.approvalService.submitForApproval(any())).thenReturn(
                ApprovalRequestResponse.builder().id("APPROVAL-2").build());

        MembershipEntity saved = service.createMembership(request);

        assertEquals("MEM-2002", saved.getMembershipNo());
        assertEquals("PENDING_APPROVAL", saved.getStatus());
        assertEquals("APPROVAL-2", saved.getApprovalRequestId());
        verify(service.approvalService).submitForApproval(any());
        verify(service.membershipChangeService).ensureBaselineHistory(saved, "actor-1");
    }

    private MembershipEntity membership(String memberId, String planId, String membershipNo) {
        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId(memberId);
        membership.setPlanId(planId);
        membership.setMembershipNo(membershipNo);
        membership.setStartDate(LocalDate.of(2026, 8, 1));
        return membership;
    }
}
