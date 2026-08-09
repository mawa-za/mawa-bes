package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipServiceIdempotencyTest {

    private MembershipRepository membershipRepository;
    private MembershipService service;

    @BeforeEach
    void setUp() {
        membershipRepository = mock(MembershipRepository.class);
        service = new MembershipService(membershipRepository);
        service.partnerRepository = mock(PartnerRepository.class);
        service.membershipPolicyConfigurationService =
                mock(MembershipPolicyConfigurationService.class);
    }

    @Test
    void returnsExistingMembershipForBlankNumberRetryOnSamePlan() {
        MembershipEntity request = membership("PARTNER-1", "PLAN-1", "");
        MembershipEntity existing = membership("PARTNER-1", "PLAN-1", "MEM-1001");
        existing.setId("SERVER-MEMBERSHIP-1");

        when(service.partnerRepository.existsById("PARTNER-1")).thenReturn(true);
        when(membershipRepository.countByMemberId("PARTNER-1")).thenReturn(1L);
        when(service.membershipPolicyConfigurationService.allowMultipleMemberships())
                .thenReturn(false);
        when(membershipRepository.findFirstByMemberIdOrderByCreatedAtDesc("PARTNER-1"))
                .thenReturn(Optional.of(existing));

        assertSame(existing, service.createMembership(request));
        verify(membershipRepository, never()).save(request);
    }

    @Test
    void keepsDifferentPlanAsAConflict() {
        MembershipEntity request = membership("PARTNER-1", "PLAN-2", "");
        MembershipEntity existing = membership("PARTNER-1", "PLAN-1", "MEM-1001");

        when(service.partnerRepository.existsById("PARTNER-1")).thenReturn(true);
        when(membershipRepository.countByMemberId("PARTNER-1")).thenReturn(1L);
        when(service.membershipPolicyConfigurationService.allowMultipleMemberships())
                .thenReturn(false);
        when(membershipRepository.findFirstByMemberIdOrderByCreatedAtDesc("PARTNER-1"))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.createMembership(request));
        verify(membershipRepository, never()).save(request);
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
