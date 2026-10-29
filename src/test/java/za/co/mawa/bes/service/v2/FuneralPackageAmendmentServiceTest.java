package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.repository.v2.*;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuneralPackageAmendmentServiceTest {
    @Mock FuneralServiceRepository funeralServices;
    @Mock FuneralPackageRepository packages;
    @Mock FuneralPackageItemRepository items;
    @Mock FuneralServiceInvoiceRepository invoiceLinks;
    @Spy ObjectMapper json = new ObjectMapper();
    @InjectMocks FuneralPackageAmendmentService service;

    @Test
    void previewKeepsExtrasAndExistingCoverSeparateFromFamilyDifference() {
        FuneralServiceEntity funeral = finalised("[ { \"description\": \"Transport\", \"amountCents\": 250000 } ]");
        givenPackages(funeral, 2000000L);
        FuneralServiceInvoiceEntity cover = new FuneralServiceInvoiceEntity();
        cover.setEntityType("BURIAL_SOCIETY");
        cover.setAmountCents(1000000L);
        when(invoiceLinks.findByFuneralServiceId("fsr-1")).thenReturn(List.of(cover));

        Map<String, Object> preview = service.preview("fsr-1", "new");
        assertEquals(2250000L, preview.get("revisedTotalCents"));
        assertEquals(750000L, preview.get("differenceCents"));
        assertEquals(1000000L, preview.get("invoicedCoverCents"));
        assertEquals(false, preview.get("requiresFundingReconciliation"));
    }

    @Test
    void downgradeBelowPaidCoverIsFlaggedForReconciliation() {
        FuneralServiceEntity funeral = finalised("[]");
        givenPackages(funeral, 400000L);
        FuneralServiceInvoiceEntity cover = new FuneralServiceInvoiceEntity();
        cover.setEntityType("GROUP_SOCIETY");
        cover.setAmountCents(1000000L);
        when(invoiceLinks.findByFuneralServiceId("fsr-1")).thenReturn(List.of(cover));
        Map<String, Object> preview = service.preview("fsr-1", "new");
        assertEquals(true, preview.get("requiresFundingReconciliation"));
    }

    @Test
    void nonFinalisedFuneralsCannotUsePostInvoiceAmendment() {
        FuneralServiceEntity funeral = finalised("[]");
        funeral.setStatus("CANCELLED");
        assertThrows(IllegalStateException.class, () -> service.preview("fsr-1", "new"));
    }

    private FuneralServiceEntity finalised(String extras) {
        FuneralServiceEntity funeral = new FuneralServiceEntity();
        funeral.setId("fsr-1");
        funeral.setStatus("INVOICED");
        funeral.setServiceRequestNo("FSR-1");
        funeral.setPackageId("old");
        funeral.setTotalAmountCents(1500000L);
        funeral.setExtrasJson(extras);
        when(funeralServices.findById("fsr-1")).thenReturn(Optional.of(funeral));
        return funeral;
    }

    private void givenPackages(FuneralServiceEntity funeral, long newBaseCents) {
        FuneralPackageEntity older = new FuneralPackageEntity();
        older.setId("old"); older.setName("Original"); older.setBasePriceCents(1000000L);
        FuneralPackageEntity newer = new FuneralPackageEntity();
        newer.setId("new"); newer.setName("Replacement"); newer.setBasePriceCents(newBaseCents);
        newer.setActive(true);
        when(packages.findById("old")).thenReturn(Optional.of(older));
        when(packages.findById("new")).thenReturn(Optional.of(newer));
        when(items.findByFuneralPackageIdOrderByProductDescriptionAsc(anyString())).thenReturn(List.of());
    }
}
