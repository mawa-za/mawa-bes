package za.co.mawa.bes.service.v2.tombstone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.v2.funeral.FuneralMembershipCoverDto;
import za.co.mawa.bes.dto.v2.funeral.FuneralServiceConfigurationDto;
import za.co.mawa.bes.dto.v2.funeral.FuneralTenantIntegrationConfigDto;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.service.v2.ApprovalService;
import za.co.mawa.bes.service.v2.CrossTenantExecutionService;
import za.co.mawa.bes.service.v2.ExternalFuneralClaimApprovalService;
import za.co.mawa.bes.service.v2.FuneralManagementService;
import za.co.mawa.bes.service.v2.MembershipClaimService;
import za.co.mawa.bes.service.v2.NumberAllocationService;
import za.co.mawa.bes.service.v2.claim.ClaimFormGenerationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TombstoneExternalCoverLookupTest {
    @Mock JdbcTemplate jdbc;
    @Mock ObjectMapper mapper;
    @Mock NumberAllocationService numbers;
    @Mock MembershipClaimService claims;
    @Mock MembershipClaimRepository claimRepository;
    @Mock FuneralManagementService funeral;
    @Mock CrossTenantExecutionService crossTenant;
    @Mock TombstoneManagementService tombstones;
    @Mock ApprovalService approvals;
    @Mock ClaimFormGenerationService forms;
    @Mock ExternalFuneralClaimApprovalService externalApprovals;
    @InjectMocks TombstoneInstallationRequestService service;

    private final LocalDate death = LocalDate.of(2026, 10, 24);

    @BeforeEach void setUp() { TenantContext.setCurrentTenant("installation_tenant"); }
    @AfterEach void tearDown() { TenantContext.clear(); }

    private FuneralMembershipCoverDto externalCover() {
        return FuneralMembershipCoverDto.builder()
                .sourceTenantId("membership_tenant")
                .sourceTenantName("Membership Association")
                .sourceMembershipId("MEM-1")
                .membershipNumber("M-0001")
                .deceasedPartnerId("DECEASED-IN-SOURCE")
                .deceasedType("MAIN_MEMBER")
                .burialSocietyName("Association")
                .funeralAmountCents(0L)  // TOMBSTONE-only plan must remain eligible.
                .build();
    }

    @SuppressWarnings("unchecked")
    private void executeSourceTenantOperation() {
        when(crossTenant.execute(eq("membership_tenant"), nullable(String.class),
                nullable(String.class), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
    }

    @Test void externalTombstoneOnlyCoverIsSelectable() {
        when(funeral.checkMembership("3409067676080")).thenReturn(List.of(externalCover()));
        executeSourceTenantOperation();
        when(claims.resolveBenefit(eq("MEM-1"), eq(MembershipClaimType.TOMBSTONE),
                eq("DECEASED-IN-SOURCE"), eq(death), isNull()))
                .thenReturn(Map.of("claimAmountCents", 180000L));
        var covers = service.checkMembership("3409067676080", death);
        assertEquals(1, covers.size());
        assertEquals("membership_tenant", covers.get(0).get("tenantId"));
        assertEquals("MEM-1", covers.get(0).get("membershipId"));
        assertEquals("DECEASED-IN-SOURCE", covers.get(0).get("deceasedPartnerId"));
        assertEquals(180000L, covers.get(0).get("tombstoneAmountCents"));
    }

    @Test void externalSourceFailureIsExplainedRatherThanSilentlyDiscardingCover() {
        when(funeral.checkMembership("3409067676080")).thenReturn(List.of(externalCover()));
        executeSourceTenantOperation();
        when(claims.resolveBenefit(anyString(), eq(MembershipClaimType.TOMBSTONE),
                anyString(), eq(death), isNull()))
                .thenThrow(new IllegalStateException("Source tenant unavailable"));
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.checkMembership("3409067676080", death));
        assertTrue(error.getMessage().contains("membership-owning tenant"));
        assertFalse(error.getMessage().contains("Enable TOMBSTONE"));
    }

    @Test void missingTombstonePayoutIsExplained() {
        when(funeral.checkMembership("3409067676080")).thenReturn(List.of(externalCover()));
        executeSourceTenantOperation();
        when(claims.resolveBenefit(anyString(), eq(MembershipClaimType.TOMBSTONE),
                anyString(), eq(death), isNull()))
                .thenThrow(new IllegalArgumentException("No payout rule configured for this plan, claim type and dependent type"));
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.checkMembership("3409067676080", death));
        assertTrue(error.getMessage().contains("active TOMBSTONE payout rule"));
    }

    @Test void noMembershipMatchReturnsEmpty() {
        when(funeral.checkMembership("unknown")).thenReturn(List.of());
        assertTrue(service.checkMembership("unknown", death).isEmpty());
    }

    @Test void configurationReportsExternalLookupStatus() {
        when(funeral.getServiceConfiguration()).thenReturn(
                FuneralServiceConfigurationDto.builder().maxSelectableCovers(3).build());
        when(funeral.getTenantIntegrationConfiguration()).thenReturn(FuneralTenantIntegrationConfigDto.builder()
                .membershipSourceMode("LOCAL_AND_EXTERNAL")
                .externalTenantId("membership_tenant")
                .active(true)
                .membershipLookupEnabled(true)
                .build());
        var config = service.configuration();
        assertEquals(3, config.get("maxSelectedCovers"));
        assertEquals("LOCAL_AND_EXTERNAL", config.get("membershipSourceMode"));
        assertEquals(true, config.get("externalLookupEnabled"));
    }
}
