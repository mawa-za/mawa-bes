package za.co.mawa.bes.service.v2.tombstone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.v2.tombstone.TombstoneDtos;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneOrderEntity;
import za.co.mawa.bes.repository.v2.tombstone.TombstoneOrderRepository;
import za.co.mawa.bes.service.v2.NumberAllocationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TombstoneOrderInitialBalanceTest {
    @Mock TombstoneOrderRepository orders;
    @Mock NumberAllocationService numbers;
    @InjectMocks TombstoneManagementService service;

    @Test
    void builderDefaultsAllRequiredAmountsBeforeFirstPersist() {
        TombstoneOrderEntity order = TombstoneOrderEntity.builder()
                .orderNo("TSO-0000000001")
                .customerPartnerId("CUSTOMER-1")
                .deceasedName("Deceased")
                .fundingMethod("CASH")
                .build();

        assertEquals("DRAFT", order.getStatus());
        assertEquals("UNFUNDED", order.getFundingStatus());
        assertEquals("NOT_STARTED", order.getProductionStatus());
        assertEquals("NOT_READY", order.getInstallationStatus());
        assertAmountsInitialized(order);
    }

    @Test
    void createOrderInitialSaveNeverContainsNullFinancialColumns() {
        when(numbers.allocateNumber("TOMBSTONE_ORDER")).thenReturn("1");

        TombstoneDtos.CreateOrderRequest request = new TombstoneDtos.CreateOrderRequest();
        request.setCustomerPartnerId("CUSTOMER-1");
        request.setDeceasedName("Deceased");
        request.setFundingMethod("CASH");
        TombstoneDtos.ItemRequest item = new TombstoneDtos.ItemRequest();
        item.setItemType("TOMBSTONE");
        item.setDescription("Granite headstone");
        item.setUnitPriceCents(150000L);
        request.setItems(List.of(item));

        // Stop at the first repository save, before any other dependencies are
        // invoked. This is the point at which MySQL previously rejected NULL balance_cents.
        doAnswer(invocation -> {
            TombstoneOrderEntity order = invocation.getArgument(0);
            assertEquals("AWAITING_FUNDING", order.getStatus());
            assertAmountsInitialized(order);
            throw new FirstSaveVerified();
        }).when(orders).save(any(TombstoneOrderEntity.class));

        assertThrows(FirstSaveVerified.class, () -> service.createOrder(request, "TEST"));
    }

    private static void assertAmountsInitialized(TombstoneOrderEntity order) {
        assertAll(
                () -> assertEquals(Long.valueOf(0L), order.getSubtotalCents()),
                () -> assertEquals(Long.valueOf(0L), order.getTaxCents()),
                () -> assertEquals(Long.valueOf(0L), order.getDiscountCents()),
                () -> assertEquals(Long.valueOf(0L), order.getTotalCents()),
                () -> assertEquals(Long.valueOf(0L), order.getConfirmedFundingCents()),
                () -> assertEquals(Long.valueOf(0L), order.getBalanceCents())
        );
    }

    private static class FirstSaveVerified extends RuntimeException { }
}
