package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimCreateRequest;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** The Claim Type Configuration toggle applies only to direct manual capture. */
@ExtendWith(MockitoExtension.class)
class MembershipClaimCreationChannelTest {
    @Mock MembershipClaimRepository claimRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock ClaimTypeConfigurationService claimTypeConfigurationService;
    @Mock ReferenceDataValidationService referenceDataValidationService;
    @Mock MembershipChangeService membershipChangeService;
    @Mock MembershipPlanClaimPayoutService payoutService;
    @InjectMocks MembershipClaimService service;

    private static final LocalDate DEATH = LocalDate.of(2026, 10, 20);

    private MembershipClaimCreateRequest request() {
        var claim = new MembershipClaimCreateRequest();
        claim.setMembershipId("MEM-1");
        claim.setClaimType(MembershipClaimType.TOMBSTONE);
        claim.setDeceasedType(MembershipClaimDeceasedType.MAIN_MEMBER);
        claim.setDeceasedPartnerId("DECEASED-1");
        claim.setDateOfDeath(DEATH);
        return claim;
    }

    @Test
    void directManualCaptureRequiresEnabledClaimType() {
        doThrow(new IllegalArgumentException("Manual TOMBSTONE claim capture disabled"))
                .when(claimTypeConfigurationService).requireEnabled(MembershipClaimType.TOMBSTONE);
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.create(request(), "actor"));
        assertTrue(error.getMessage().contains("disabled"));
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void addingInstallationRequestIdToDirectApiDoesNotBypassPermission() {
        var forged = request();
        forged.setTombstoneInstallationRequestId("INSTALL-1");
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.create(forged, "actor"));
        assertTrue(error.getMessage().contains("installation workflow"));
        verifyNoInteractions(membershipRepository, claimTypeConfigurationService);
    }

    @Test
    void authorisedInstallationWorkflowIgnoresDirectCaptureToggleButChecksMembership() {
        var workflow = request();
        workflow.setTombstoneInstallationRequestId("INSTALL-1");
        when(membershipRepository.findByIdForUpdate("MEM-1")).thenReturn(Optional.empty());
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.createFromTombstoneInstallationRequest(workflow, "actor"));
        assertTrue(error.getMessage().contains("Membership not found"));
        verifyNoInteractions(claimTypeConfigurationService);
        verify(membershipRepository).findByIdForUpdate("MEM-1");
    }

    @Test
    void workflowBypassRequiresTombstoneClaimAndInstallationReference() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createFromTombstoneInstallationRequest(request(), "actor"));
        var invalid = request();
        invalid.setTombstoneInstallationRequestId("INSTALL-1");
        invalid.setClaimType(MembershipClaimType.FUNERAL);
        assertThrows(IllegalArgumentException.class,
                () -> service.createFromTombstoneInstallationRequest(invalid, "actor"));
        verifyNoInteractions(membershipRepository, claimTypeConfigurationService);
    }

    @Test
    void planEntitlementLookupDoesNotUseDirectCaptureConfiguration() {
        var member = new MembershipEntity();
        member.setId("MEM-1");
        member.setMemberId("DECEASED-1");
        when(membershipRepository.findById("MEM-1")).thenReturn(Optional.of(member));
        when(membershipChangeService.resolveCoveragePlanId("MEM-1", DEATH, "actor"))
                .thenReturn("PLAN-1");
        when(payoutService.resolvePayoutAmountCents(
                "PLAN-1", MembershipClaimType.TOMBSTONE, DependentType.MAIN_MEMBER))
                .thenReturn(180000L);
        var benefit = service.resolveBenefit("MEM-1", MembershipClaimType.TOMBSTONE,
                "DECEASED-1", DEATH, "actor");
        assertEquals(180000L, benefit.get("claimAmountCents"));
        verifyNoInteractions(claimTypeConfigurationService);
    }
}
