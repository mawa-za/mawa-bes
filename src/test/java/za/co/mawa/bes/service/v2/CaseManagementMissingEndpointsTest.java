package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import za.co.mawa.bes.dto.v2.CaseEventCreateRequest;
import za.co.mawa.bes.dto.v2.CasePartyCreateRequest;
import za.co.mawa.bes.entity.v2.CaseEventEntity;
import za.co.mawa.bes.entity.v2.CasePartyEntity;
import za.co.mawa.bes.entity.v2.LegalCaseEntity;
import za.co.mawa.bes.enums.CaseEventStatus;
import za.co.mawa.bes.enums.CaseEventType;
import za.co.mawa.bes.enums.CasePartyType;
import za.co.mawa.bes.repository.v2.CaseDisbursementRepository;
import za.co.mawa.bes.repository.v2.CaseEventRepository;
import za.co.mawa.bes.repository.v2.CasePartyRepository;
import za.co.mawa.bes.repository.v2.CaseTaskRepository;
import za.co.mawa.bes.repository.v2.CaseTimeEntryRepository;
import za.co.mawa.bes.repository.v2.LegalCaseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class CaseManagementMissingEndpointsTest {

    @Test
    void unbilledMattersExcludeFullyBilledAndNonBillableCases() {
        LegalCaseRepository cases = mock(LegalCaseRepository.class);
        CaseDashboardService service = new CaseDashboardService(cases,
                mock(CaseTaskRepository.class), mock(CaseEventRepository.class),
                mock(CaseTimeEntryRepository.class), mock(CaseDisbursementRepository.class));
        LegalCaseEntity open = new LegalCaseEntity();
        open.setBillable(true);
        open.setBalanceCents(5000L);
        LegalCaseEntity invoiced = new LegalCaseEntity();
        invoiced.setBillable(true);
        invoiced.setBalanceCents(0L);
        LegalCaseEntity excluded = new LegalCaseEntity();
        excluded.setBillable(false);
        excluded.setBalanceCents(10000L);
        when(cases.findAll()).thenReturn(List.of(open, invoiced, excluded));

        assertEquals(List.of(open), service.getUnbilledCases());
    }

    @Test
    void updatingPartyRetainsCaseRelationship() {
        CasePartyRepository repository = mock(CasePartyRepository.class);
        ReferenceDataValidationService validation = mock(ReferenceDataValidationService.class);
        CasePartyService service = new CasePartyService(repository,
                mock(LegalCaseService.class), validation);
        CasePartyEntity existing = new CasePartyEntity();
        existing.setId("party-1");
        existing.setCaseId("case-1");
        when(repository.findById("party-1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(validation.optionalContactNumber("0123456789")).thenReturn("0123456789");

        CasePartyCreateRequest update = new CasePartyCreateRequest();
        update.setPartyName("Updated party");
        update.setPartyType(CasePartyType.WITNESS);
        update.setPhoneNumber("0123456789");

        assertSame(existing, service.update("party-1", update));
        assertEquals("case-1", existing.getCaseId());
        assertEquals("Updated party", existing.getPartyName());
        assertEquals(CasePartyType.WITNESS, existing.getPartyType());
        verify(repository).save(existing);
    }

    @Test
    void updatingEventPreservesStatusAndCaseRelationship() {
        CaseEventRepository repository = mock(CaseEventRepository.class);
        CaseEventService service = new CaseEventService(repository, mock(LegalCaseService.class));
        CaseEventEntity existing = new CaseEventEntity();
        existing.setId("event-1");
        existing.setCaseId("case-1");
        existing.setStatus(CaseEventStatus.SCHEDULED);
        when(repository.findById("event-1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        CaseEventCreateRequest update = new CaseEventCreateRequest();
        update.setEventType(CaseEventType.MEETING);
        update.setTitle("Updated hearing");
        update.setStartAt(LocalDateTime.of(2026, 10, 29, 13, 30));

        assertSame(existing, service.update("event-1", update));
        assertEquals("case-1", existing.getCaseId());
        assertEquals(CaseEventStatus.SCHEDULED, existing.getStatus());
        assertEquals("Updated hearing", existing.getTitle());
        verify(repository).save(existing);
    }
}
