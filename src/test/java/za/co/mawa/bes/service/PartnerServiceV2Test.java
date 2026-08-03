package za.co.mawa.bes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.PartnerViewEntity;
import za.co.mawa.bes.repository.PartnerViewRepository;

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

    private PartnerServiceV2 service;

    @BeforeEach
    void setUp() {
        service = new PartnerServiceV2();
        service.partnerViewRepository = partnerViewRepository;
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
}
