package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CaseBillingSummaryResponse;
import za.co.mawa.bes.entity.v2.CaseDisbursementEntity;
import za.co.mawa.bes.entity.v2.CaseTimeEntryEntity;
import za.co.mawa.bes.entity.v2.LegalCaseEntity;
import za.co.mawa.bes.repository.v2.CaseDisbursementRepository;
import za.co.mawa.bes.repository.v2.CaseTimeEntryRepository;
import za.co.mawa.bes.repository.v2.LegalCaseRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseBillingService {
    private final LegalCaseRepository legalCaseRepository;
    private final CaseTimeEntryRepository caseTimeEntryRepository;
    private final CaseDisbursementRepository caseDisbursementRepository;

    @Transactional
    public void recalculateCaseTotals(String caseId) {
        LegalCaseEntity legalCase = legalCaseRepository.findById(caseId).orElseThrow(() -> new RuntimeException("Case not found"));
        List<CaseTimeEntryEntity> timeEntries = caseTimeEntryRepository.findByCaseIdOrderByEntryDateDesc(caseId);
        List<CaseDisbursementEntity> disbursements = caseDisbursementRepository.findByCaseIdOrderByDisbursementDateDesc(caseId);
        long totalTimeMinutes = timeEntries.stream().mapToLong(e -> e.getMinutes() != null ? e.getMinutes() : 0L).sum();
        long totalFeesCents = timeEntries.stream().filter(e -> Boolean.TRUE.equals(e.getBillable())).mapToLong(e -> e.getAmountCents() != null ? e.getAmountCents() : 0L).sum();
        long totalDisbursementsCents = disbursements.stream().filter(e -> Boolean.TRUE.equals(e.getBillable())).mapToLong(e -> e.getAmountCents() != null ? e.getAmountCents() : 0L).sum();
        long totalBillableCents = totalFeesCents + totalDisbursementsCents;
        long totalBilledFees = timeEntries.stream().filter(e -> Boolean.TRUE.equals(e.getBilled())).mapToLong(e -> e.getAmountCents() != null ? e.getAmountCents() : 0L).sum();
        long totalBilledDisbursements = disbursements.stream().filter(e -> Boolean.TRUE.equals(e.getBilled())).mapToLong(e -> e.getAmountCents() != null ? e.getAmountCents() : 0L).sum();
        long totalBilledCents = totalBilledFees + totalBilledDisbursements;
        legalCase.setTotalTimeMinutes(totalTimeMinutes);
        legalCase.setTotalFeesCents(totalFeesCents);
        legalCase.setTotalDisbursementsCents(totalDisbursementsCents);
        legalCase.setTotalBillableCents(totalBillableCents);
        legalCase.setTotalBilledCents(totalBilledCents);
        legalCase.setBalanceCents(totalBillableCents - totalBilledCents);
        legalCaseRepository.save(legalCase);
    }

    public CaseBillingSummaryResponse getBillingSummary(String caseId) {
        LegalCaseEntity legalCase = legalCaseRepository.findById(caseId).orElseThrow(() -> new RuntimeException("Case not found"));
        List<CaseTimeEntryEntity> timeEntries = caseTimeEntryRepository.findByCaseIdOrderByEntryDateDesc(caseId);
        List<CaseDisbursementEntity> disbursements = caseDisbursementRepository.findByCaseIdOrderByDisbursementDateDesc(caseId);
        long unbilledTimeMinutes = timeEntries.stream().filter(e -> Boolean.TRUE.equals(e.getBillable())).filter(e -> !Boolean.TRUE.equals(e.getBilled())).mapToLong(e -> e.getMinutes() != null ? e.getMinutes() : 0L).sum();
        long unbilledFeesCents = timeEntries.stream().filter(e -> Boolean.TRUE.equals(e.getBillable())).filter(e -> !Boolean.TRUE.equals(e.getBilled())).mapToLong(e -> e.getAmountCents() != null ? e.getAmountCents() : 0L).sum();
        long unbilledDisbursementsCents = disbursements.stream().filter(e -> Boolean.TRUE.equals(e.getBillable())).filter(e -> !Boolean.TRUE.equals(e.getBilled())).mapToLong(e -> e.getAmountCents() != null ? e.getAmountCents() : 0L).sum();
        return CaseBillingSummaryResponse.builder().caseId(legalCase.getId()).caseNo(legalCase.getCaseNo()).title(legalCase.getTitle()).totalTimeMinutes(legalCase.getTotalTimeMinutes()).unbilledTimeMinutes(unbilledTimeMinutes).totalFeesCents(legalCase.getTotalFeesCents()).unbilledFeesCents(unbilledFeesCents).totalDisbursementsCents(legalCase.getTotalDisbursementsCents()).unbilledDisbursementsCents(unbilledDisbursementsCents).totalBillableCents(legalCase.getTotalBillableCents()).totalBilledCents(legalCase.getTotalBilledCents()).balanceCents(legalCase.getBalanceCents()).build();
    }
}
