package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CaseTimeEntryCreateRequest;
import za.co.mawa.bes.entity.v2.CaseTimeEntryEntity;
import za.co.mawa.bes.repository.v2.CaseTimeEntryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseTimeEntryService {
    private final CaseTimeEntryRepository caseTimeEntryRepository;
    private final LegalCaseService legalCaseService;
    private final CaseBillingService caseBillingService;

    @Transactional
    public CaseTimeEntryEntity create(String caseId, CaseTimeEntryCreateRequest request, String createdBy) {
        legalCaseService.findById(caseId);
        long amountCents = Math.round((request.getHourlyRateCents() / 60.0) * request.getMinutes());
        CaseTimeEntryEntity entity = new CaseTimeEntryEntity();
        entity.setCaseId(caseId);
        entity.setTaskId(request.getTaskId());
        entity.setEntryDate(request.getEntryDate() != null ? request.getEntryDate() : LocalDate.now());
        entity.setUserId(request.getUserId());
        entity.setDescription(request.getDescription());
        entity.setMinutes(request.getMinutes());
        entity.setHourlyRateCents(request.getHourlyRateCents());
        entity.setAmountCents(amountCents);
        if (request.getBillable() != null) entity.setBillable(request.getBillable());
        entity.setBilled(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        CaseTimeEntryEntity saved = caseTimeEntryRepository.save(entity);
        caseBillingService.recalculateCaseTotals(caseId);
        return saved;
    }

    public List<CaseTimeEntryEntity> findByCaseId(String caseId) { return caseTimeEntryRepository.findByCaseIdOrderByEntryDateDesc(caseId); }
}
