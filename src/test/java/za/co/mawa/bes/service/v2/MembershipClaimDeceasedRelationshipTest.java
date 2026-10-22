package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimCreateRequest;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.enums.MembershipDependentStatus;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipDependentRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipClaimDeceasedRelationshipTest {
    @Mock MembershipClaimRepository claimRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock MembershipDependentRepository dependentRepository;
    @Mock PartnerRepository partnerRepository;
    @Mock ClaimTypeConfigurationService claimTypeConfigurationService;
    @Mock ReferenceDataValidationService referenceDataValidationService;
    @Mock MembershipActionGuardService membershipActionGuardService;
    @Mock NumberAllocationService numberAllocationService;
    @InjectMocks MembershipClaimService service;

    @Test
    void mainMemberDeceasedFlagIsMembershipScoped() {
        MembershipEntity membership = new MembershipEntity();
        membership.setId("MEM-1");
        membership.setMemberId("PARTNER-1");
        when(membershipRepository.findById("MEM-1")).thenReturn(Optional.of(membership));
        MembershipClaimEntity claim = claim(MembershipClaimDeceasedType.MAIN_MEMBER);

        ReflectionTestUtils.invokeMethod(service, "markDeceasedOnMembership", claim, "actor");

        assertEquals("DECEASED", membership.getMemberStatus());
        assertEquals(LocalDate.of(2026, 10, 20), membership.getMemberDeceasedDate());
        verify(membershipRepository).save(membership);
        verifyNoInteractions(partnerRepository, dependentRepository);
    }

    @Test
    void dependentIsMarkedDeceasedWithoutChangingGlobalPartner() {
        MembershipDependentEntity dependent = new MembershipDependentEntity();
        dependent.setMembershipId("MEM-1");
        dependent.setDependentPartnerId("PARTNER-1");
        dependent.setActive(true);
        dependent.setStatus(MembershipDependentStatus.ACTIVE);
        when(dependentRepository.findFirstByMembershipIdAndDependentPartnerIdOrderByCreatedAtDesc(
                "MEM-1", "PARTNER-1")).thenReturn(Optional.of(dependent));

        ReflectionTestUtils.invokeMethod(service, "markDeceasedOnMembership",
                claim(MembershipClaimDeceasedType.DEPENDENT), "actor");

        assertEquals(MembershipDependentStatus.DECEASED, dependent.getStatus());
        assertFalse(dependent.getActive());
        assertEquals(LocalDate.of(2026, 10, 20), dependent.getDeceasedDate());
        verify(dependentRepository).save(dependent);
        verifyNoInteractions(partnerRepository, membershipRepository);
    }

    @Test
    void newClaimIsBlockedEvenWhileEarlierClaimIsDraft() {
        MembershipEntity membership = membership("ACTIVE");
        when(membershipRepository.findByIdForUpdate("MEM-1")).thenReturn(Optional.of(membership));
        when(claimRepository.existsByMembershipIdAndDeceasedPartnerIdAndStatusNot(
                "MEM-1", "PARTNER-1", MembershipClaimStatus.CANCELLED)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request(), "actor"));
        assertTrue(ex.getMessage().contains("cancelled"));
        verifyNoInteractions(numberAllocationService);
    }

    @Test
    void deceasedMemberWithoutCancelledClaimCannotBeClaimedAgain() {
        when(membershipRepository.findByIdForUpdate("MEM-1"))
                .thenReturn(Optional.of(membership("DECEASED")));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request(), "actor"));
        assertTrue(ex.getMessage().contains("no cancelled claim"));
    }

    private MembershipEntity membership(String memberStatus) {
        MembershipEntity membership = new MembershipEntity();
        membership.setId("MEM-1");
        membership.setMemberId("PARTNER-1");
        membership.setMemberStatus(memberStatus);
        return membership;
    }

    private MembershipClaimCreateRequest request() {
        MembershipClaimCreateRequest request = new MembershipClaimCreateRequest();
        request.setMembershipId("MEM-1");
        request.setDeceasedPartnerId("PARTNER-1");
        request.setDeceasedType(MembershipClaimDeceasedType.MAIN_MEMBER);
        request.setClaimType(MembershipClaimType.FUNERAL);
        request.setDateOfDeath(LocalDate.of(2026, 10, 20));
        return request;
    }

    private MembershipClaimEntity claim(MembershipClaimDeceasedType type) {
        MembershipClaimEntity claim = new MembershipClaimEntity();
        claim.setMembershipId("MEM-1");
        claim.setDeceasedPartnerId("PARTNER-1");
        claim.setDeceasedType(type);
        claim.setDateOfDeath(LocalDate.of(2026, 10, 20));
        claim.setClaimNo("CLAIM-100");
        return claim;
    }
}
