package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import za.co.mawa.bes.dto.partner.PartnerEditDto;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.repository.PartnerRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerServiceEditTest {

    @Test
    void editLoadsManagedEntityInsteadOfLazyReferenceAndFlushesChanges() {
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        PartnerEntity partner = new PartnerEntity();
        partner.setId("partner-1");
        partner.setName1("OLD");
        when(partnerRepository.findById("partner-1")).thenReturn(Optional.of(partner));

        PartnerService service = new PartnerService();
        service.partnerRepository = partnerRepository;

        PartnerEditDto request = new PartnerEditDto();
        request.setId("partner-1");
        request.setName1("Updated name");
        request.setGender("FEMALE");

        service.edit(request);

        assertEquals("UPDATED NAME", partner.getName1());
        assertEquals("FEMALE", partner.getGender());
        verify(partnerRepository).findById("partner-1");
        verify(partnerRepository).saveAndFlush(partner);
    }

    @Test
    void editRejectsUnknownPartnerWithoutSaving() {
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        when(partnerRepository.findById("missing")).thenReturn(Optional.empty());

        PartnerService service = new PartnerService();
        service.partnerRepository = partnerRepository;

        PartnerEditDto request = new PartnerEditDto();
        request.setId("missing");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.edit(request));

        assertEquals("Partner not found: missing", error.getMessage());
        verify(partnerRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
}
