package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CaseInvoiceGenerateRequest;
import za.co.mawa.bes.dto.v2.CaseInvoiceGenerationResponse;
import za.co.mawa.bes.dto.v2.CaseInvoiceLineResponse;
import za.co.mawa.bes.entity.v2.CaseDisbursementEntity;
import za.co.mawa.bes.entity.v2.CaseTimeEntryEntity;
import za.co.mawa.bes.entity.v2.LegalCaseEntity;
import za.co.mawa.bes.repository.v2.CaseDisbursementRepository;
import za.co.mawa.bes.repository.v2.CaseTimeEntryRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseInvoiceGenerationService {

    private final LegalCaseService legalCaseService;
    private final CaseBillingService caseBillingService;
    private final CaseTimeEntryRepository caseTimeEntryRepository;
    private final CaseDisbursementRepository caseDisbursementRepository;

    /**
     * This method prepares invoice-ready lines from unbilled time and disbursements.
     * If your existing invoice module creates the invoice first, call this endpoint without invoiceId
     * to preview lines, create the invoice from those lines, then call it again with invoiceId and markAsBilled=true.
     */
    @Transactional
    public CaseInvoiceGenerationResponse generateInvoice(String caseId, CaseInvoiceGenerateRequest request) {
        LegalCaseEntity legalCase = legalCaseService.findById(caseId);

        List<CaseTimeEntryEntity> timeEntries = caseTimeEntryRepository.findByCaseIdAndBilledFalse(caseId);
        List<CaseDisbursementEntity> disbursements = caseDisbursementRepository.findByCaseIdAndBilledFalse(caseId);

        List<CaseInvoiceLineResponse> lines = new ArrayList<>();

        for (CaseTimeEntryEntity entry : timeEntries) {
            if (!Boolean.TRUE.equals(entry.getBillable())) {
                continue;
            }
            lines.add(CaseInvoiceLineResponse.builder()
                    .sourceType("TIME_ENTRY")
                    .sourceId(entry.getId())
                    .description(entry.getDescription())
                    .quantityMinutes(entry.getMinutes())
                    .unitPriceCents(entry.getHourlyRateCents())
                    .amountCents(entry.getAmountCents())
                    .build());
        }

        for (CaseDisbursementEntity item : disbursements) {
            if (!Boolean.TRUE.equals(item.getBillable())) {
                continue;
            }
            lines.add(CaseInvoiceLineResponse.builder()
                    .sourceType("DISBURSEMENT")
                    .sourceId(item.getId())
                    .description(item.getDescription())
                    .quantityMinutes(null)
                    .unitPriceCents(item.getAmountCents())
                    .amountCents(item.getAmountCents())
                    .build());
        }

        long totalAmountCents = lines.stream()
                .mapToLong(line -> line.getAmountCents() != null ? line.getAmountCents() : 0L)
                .sum();

        boolean markAsBilled = Boolean.TRUE.equals(request.getMarkAsBilled()) && request.getInvoiceId() != null;

        if (markAsBilled) {
            for (CaseTimeEntryEntity entry : timeEntries) {
                if (Boolean.TRUE.equals(entry.getBillable())) {
                    entry.setBilled(true);
                    entry.setInvoiceId(request.getInvoiceId());
                    caseTimeEntryRepository.save(entry);
                }
            }

            for (CaseDisbursementEntity item : disbursements) {
                if (Boolean.TRUE.equals(item.getBillable())) {
                    item.setBilled(true);
                    item.setInvoiceId(request.getInvoiceId());
                    caseDisbursementRepository.save(item);
                }
            }

            caseBillingService.recalculateCaseTotals(caseId);
        }

        return CaseInvoiceGenerationResponse.builder()
                .caseId(legalCase.getId())
                .caseNo(legalCase.getCaseNo())
                .title(legalCase.getTitle())
                .invoiceId(request.getInvoiceId())
                .lineCount((long) lines.size())
                .totalAmountCents(totalAmountCents)
                .markedAsBilled(markAsBilled)
                .lines(lines)
                .build();
    }
}
