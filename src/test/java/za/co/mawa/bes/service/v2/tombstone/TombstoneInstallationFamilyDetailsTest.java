package za.co.mawa.bes.service.v2.tombstone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.dto.v2.funeral.FuneralServiceConfigurationDto;
import za.co.mawa.bes.dto.v2.tombstone.TombstoneDtos;
import za.co.mawa.bes.dto.v2.tombstone.TombstoneInstallationDtos;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.service.v2.ApprovalService;
import za.co.mawa.bes.service.v2.CrossTenantExecutionService;
import za.co.mawa.bes.service.v2.ExternalFuneralClaimApprovalService;
import za.co.mawa.bes.service.v2.FuneralManagementService;
import za.co.mawa.bes.service.v2.MembershipClaimService;
import za.co.mawa.bes.service.v2.NumberAllocationService;
import za.co.mawa.bes.service.v2.claim.ClaimFormGenerationService;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TombstoneInstallationFamilyDetailsTest {
    @Mock JdbcTemplate jdbc;
    @Spy ObjectMapper mapper = new ObjectMapper();
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

    private TombstoneInstallationDtos.Request familyRequest() {
        TombstoneInstallationDtos.Request request = new TombstoneInstallationDtos.Request();
        request.setCustomerPartnerId("FAMILY-REP-1");
        request.setFamilyRepresentativeName("Family Representative");
        request.setDeceasedName("Family-Supplied Deceased Name");
        TombstoneDtos.ItemRequest item = new TombstoneDtos.ItemRequest();
        item.setDescription("Tombstone and inscription");
        item.setItemType("TOMBSTONE");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPriceCents(250000L);
        request.setItems(List.of(item));
        request.setSelectedCovers(new ArrayList<>());
        return request;
    }

    private void setCoverLimit() {
        when(funeral.getServiceConfiguration()).thenReturn(
                FuneralServiceConfigurationDto.builder().maxSelectableCovers(3).build());
    }

    @Test
    void acceptsFamilyDetailsWithoutDeceasedPartnerDeathCertificateDateOrMembership() {
        setCoverLimit();
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validate", familyRequest()));
    }

    @Test
    void membershipClaimStillNeedsDateOfDeath() {
        TombstoneInstallationDtos.Request request = familyRequest();
        request.getSelectedCovers().add(new TombstoneInstallationDtos.SelectedCover());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validate", request));
        assertTrue(exception.getMessage().contains("Date of death"));
    }

    @Test
    void rejectsFutureDeathDateEvenWithoutClaim() {
        TombstoneInstallationDtos.Request request = familyRequest();
        request.setDateOfDeath(LocalDate.now().plusDays(1));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validate", request));
        assertTrue(exception.getMessage().contains("future"));
    }

    @Test
    void onlyOneTombstonePackagePerDeceasedIsAccepted() {
        TombstoneInstallationDtos.Request request = familyRequest();
        TombstoneDtos.ItemRequest duplicate = new TombstoneDtos.ItemRequest();
        duplicate.setItemType("TOMBSTONE");
        duplicate.setDescription("Second tombstone");
        duplicate.setUnitPriceCents(150000L);
        request.setItems(List.of(request.getItems().get(0), duplicate));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validate", request));
        assertTrue(error.getMessage().contains("exactly one"));
    }

    @Test
    void rejectsQuantitiesOtherThanOne() {
        TombstoneInstallationDtos.Request request = familyRequest();
        request.getItems().get(0).setQuantity(BigDecimal.valueOf(2));
        setCoverLimit();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validate", request));
        assertTrue(error.getMessage().contains("quantity must be one"));
    }

    @Test
    void customerFundedInstallationGeneratesCashOrderWithoutClaim() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", "REQ-1");
        row.put("request_no", "TIR-00000001");
        row.put("status", "DRAFT");
        row.put("customer_partner_id", "FAMILY-REP-1");
        row.put("deceased_partner_id", null);
        row.put("deceased_name", "Family-Supplied Deceased Name");
        row.put("discount_cents", 0L);
        row.put("installation_directions", "Left after the community hall, blue gate");
        row.put("items_json", "[{\"description\":\"Memorial\",\"quantity\":1,\"unitPriceCents\":250000}]");
        row.put("covers_json", "[]");
        when(jdbc.queryForList(anyString(), eq("REQ-1"))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            return sql.contains("tombstone_installation_request_claim") ? List.of() : List.of(row);
        });
        when(tombstones.createOrder(any(), eq("tester"))).thenReturn(
                TombstoneDtos.OrderResponse.builder().id("ORDER-1").build());

        service.generate("REQ-1", "tester");

        ArgumentCaptor<TombstoneDtos.CreateOrderRequest> order =
                ArgumentCaptor.forClass(TombstoneDtos.CreateOrderRequest.class);
        verify(tombstones).createOrder(order.capture(), eq("tester"));
        assertEquals("CASH", order.getValue().getFundingMethod());
        assertNull(order.getValue().getDeceasedPartnerId());
        assertEquals("Family-Supplied Deceased Name", order.getValue().getDeceasedName());
        assertTrue(order.getValue().getNotes().contains("Left after the community hall, blue gate"));
        verifyNoInteractions(claims);
    }
}
