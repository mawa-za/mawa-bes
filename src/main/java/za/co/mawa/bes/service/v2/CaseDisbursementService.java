package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CaseDisbursementCreateRequest;
import za.co.mawa.bes.entity.v2.CaseDisbursementEntity;
import za.co.mawa.bes.repository.v2.CaseDisbursementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseDisbursementService {
    private final CaseDisbursementRepository caseDisbursementRepository;
    private final LegalCaseService legalCaseService;
    private final CaseBillingService caseBillingService;

    @Transactional
    public CaseDisbursementEntity create(String caseId, CaseDisbursementCreateRequest request, String createdBy) {
        legalCaseService.findById(caseId);
        CaseDisbursementEntity entity = new CaseDisbursementEntity();
        entity.setCaseId(caseId);
        entity.setDisbursementDate(request.getDisbursementDate() != null ? request.getDisbursementDate() : LocalDate.now());
        entity.setDisbursementType(request.getDisbursementType());
        entity.setDescription(request.getDescription());
        entity.setAmountCents(request.getAmountCents());
        if (request.getBillable() != null) entity.setBillable(request.getBillable());
        entity.setBilled(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        CaseDisbursementEntity saved = caseDisbursementRepository.save(entity);
        caseBillingService.recalculateCaseTotals(caseId);
        return saved;
    }

    public List<CaseDisbursementEntity> findByCaseId(String caseId) { return caseDisbursementRepository.findByCaseIdOrderByDisbursementDateDesc(caseId); }
}
