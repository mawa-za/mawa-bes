package za.co.mawa.bes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import za.co.mawa.bes.dto.partner.PartnerIdentityDto;
import za.co.mawa.bes.dto.partner.PartnerInboundDto;
import za.co.mawa.bes.entity.PartnerContactEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerViewEntity;
import za.co.mawa.bes.repository.PartnerContactRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.PartnerViewRepository;
import za.co.mawa.bes.service.v2.ReferenceDataValidationService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerServiceV2Test {

    @Mock
    private PartnerViewRepository partnerViewRepository;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private PartnerIdentityServiceV2 partnerIdentityServiceV2;
    @Mock
    private PartnerContactRepository partnerContactRepository;
    @Mock
    private ReferenceDataValidationService referenceDataValidationService;

    private PartnerServiceV2 service;

    @BeforeEach
    void setUp() {
        service = new PartnerServiceV2();
        service.partnerViewRepository = partnerViewRepository;
        service.partnerRepository = partnerRepository;
        service.partnerIdentityServiceV2 = partnerIdentityServiceV2;
        service.partnerContactRepository = partnerContactRepository;
        service.referenceDataValidationService = referenceDataValidationService;
    }

    @Test
    void getByIdReturnsMaterialisedPartnerInsteadOfDetachedReference() {
        PartnerViewEntity partner = PartnerViewEntity.builder()
                .partnerId("partner-1")
                .partnerNo("100001")
                .build();
        when(partnerViewRepository.findById("partner-1")).thenReturn(Optional.of(partner));

        PartnerViewEntity result = service.getById("partner-1");

        assertSame(partner, result);
        verify(partnerViewRepository).findById("partner-1");
        verify(partnerViewRepository, never()).getReferenceById("partner-1");
    }

    @Test
    void getByIdRejectsMissingPartnerWithUsefulMessage() {
        when(partnerViewRepository.findById("missing")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getById("missing")
        );

        org.junit.jupiter.api.Assertions.assertEquals("Partner not found: missing", exception.getMessage());
    }
    @Test
    void createEnrichesExistingIdentityWithMiddleNameAndCellphone() {
        PartnerIdentityDto identity = new PartnerIdentityDto();
        identity.setPartner("partner-1");
        PartnerEntity partner = PartnerEntity.builder()
                .id("partner-1")
                .name1("FIRST")
                .name2("LAST")
                .type("INDIVIDUAL")
                .build();
        PartnerViewEntity view = PartnerViewEntity.builder()
                .partnerId("partner-1")
                .partnerNo("100001")
                .name3("MIDDLE")
                .build();

        when(partnerIdentityServiceV2.getIdentity("SA-ID", "9001015009087"))
                .thenReturn(identity);
        when(partnerRepository.findById("partner-1")).thenReturn(Optional.of(partner));
        when(referenceDataValidationService.requireContactNumber("082 123 4567"))
                .thenReturn("0821234567");
        when(partnerViewRepository.findById("partner-1")).thenReturn(Optional.of(view));

        PartnerInboundDto request = new PartnerInboundDto();
        request.setIdentityType("SA-ID");
        request.setIdentityNumber("9001015009087");
        request.setName1("First");
        request.setName2("Last");
        request.setName3("Middle");
        request.setContactNumber("082 123 4567");

        PartnerViewEntity result = service.create(request);

        assertSame(view, result);
        org.junit.jupiter.api.Assertions.assertEquals("MIDDLE", partner.getName3());
        verify(partnerRepository).save(partner);

        ArgumentCaptor<PartnerContactEntity> contactCaptor = ArgumentCaptor.forClass(PartnerContactEntity.class);
        verify(partnerContactRepository).save(contactCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                "CELLPHONE",
                contactCaptor.getValue().getPartnerContactPK().getType()
        );
        org.junit.jupiter.api.Assertions.assertEquals("partner-1", contactCaptor.getValue().getPartnerContactPK().getPartner());
        org.junit.jupiter.api.Assertions.assertEquals("0821234567", contactCaptor.getValue().getValue());
    }

}
