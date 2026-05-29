package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.CaseDashboardResponse;
import za.co.mawa.bes.entity.v2.CaseDisbursementEntity;
import za.co.mawa.bes.entity.v2.CaseEventEntity;
import za.co.mawa.bes.entity.v2.CaseTaskEntity;
import za.co.mawa.bes.entity.v2.CaseTimeEntryEntity;
import za.co.mawa.bes.enums.CaseEventStatus;
import za.co.mawa.bes.enums.CaseTaskStatus;
import za.co.mawa.bes.enums.LegalCaseStatus;
import za.co.mawa.bes.repository.v2.CaseDisbursementRepository;
import za.co.mawa.bes.repository.v2.CaseEventRepository;
import za.co.mawa.bes.repository.v2.CaseTaskRepository;
import za.co.mawa.bes.repository.v2.CaseTimeEntryRepository;
import za.co.mawa.bes.repository.v2.LegalCaseRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseDashboardService {

    private final LegalCaseRepository legalCaseRepository;
    private final CaseTaskRepository caseTaskRepository;
    private final CaseEventRepository caseEventRepository;
    private final CaseTimeEntryRepository caseTimeEntryRepository;
    private final CaseDisbursementRepository caseDisbursementRepository;

    public CaseDashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next14Days = now.plusDays(14);

        List<CaseTaskEntity> overdueTasks = getOverdueTasks();
        List<CaseEventEntity> upcomingEvents = getUpcomingEvents(now, next14Days);
        List<CaseTimeEntryEntity> unbilledTime = getUnbilledTimeEntries(null);
        List<CaseDisbursementEntity> unbilledDisbursements = getUnbilledDisbursements(null);

        long unbilledTimeAmount = unbilledTime.stream()
                .filter(e -> Boolean.TRUE.equals(e.getBillable()))
                .mapToLong(e -> e.getAmountCents() != null ? e.getAmountCents() : 0L)
                .sum();

        long unbilledDisbursementAmount = unbilledDisbursements.stream()
                .filter(e -> Boolean.TRUE.equals(e.getBillable()))
                .mapToLong(e -> e.getAmountCents() != null ? e.getAmountCents() : 0L)
                .sum();

        return CaseDashboardResponse.builder()
                .totalCases(legalCaseRepository.count())
                .openCases(legalCaseRepository.countByStatus(LegalCaseStatus.OPEN))
                .inProgressCases(legalCaseRepository.countByStatus(LegalCaseStatus.IN_PROGRESS))
                .onHoldCases(legalCaseRepository.countByStatus(LegalCaseStatus.ON_HOLD))
                .closedCases(legalCaseRepository.countByStatus(LegalCaseStatus.CLOSED))
                .overdueTasks((long) overdueTasks.size())
                .upcomingEvents((long) upcomingEvents.size())
                .unbilledTimeEntries((long) unbilledTime.size())
                .unbilledDisbursements((long) unbilledDisbursements.size())
                .unbilledAmountCents(unbilledTimeAmount + unbilledDisbursementAmount)
                .build();
    }

    public List<CaseTaskEntity> getOverdueTasks() {
        return caseTaskRepository.findByStatusNotAndDueDateBeforeOrderByDueDateAsc(
                CaseTaskStatus.COMPLETED,
                LocalDateTime.now()
        );
    }

    public List<CaseEventEntity> getUpcomingEvents(LocalDateTime from, LocalDateTime to) {
        return caseEventRepository.findByStatusAndStartAtBetweenOrderByStartAtAsc(
                CaseEventStatus.SCHEDULED,
                from,
                to
        );
    }

    public List<CaseTimeEntryEntity> getUnbilledTimeEntries(String caseId) {
        if (caseId != null) {
            return caseTimeEntryRepository.findByCaseIdAndBilledFalse(caseId);
        }
        return caseTimeEntryRepository.findAll().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getBilled()))
                .toList();
    }

    public List<CaseDisbursementEntity> getUnbilledDisbursements(String caseId) {
        if (caseId != null) {
            return caseDisbursementRepository.findByCaseIdAndBilledFalse(caseId);
        }
        return caseDisbursementRepository.findAll().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getBilled()))
                .toList();
    }
}
