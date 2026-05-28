package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.LegalCaseCreateRequest;
import za.co.mawa.bes.dto.v2.LegalCaseUpdateRequest;
import za.co.mawa.bes.entity.v2.LegalCaseEntity;
import za.co.mawa.bes.enums.LegalCaseStatus;
import za.co.mawa.bes.repository.v2.LegalCaseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LegalCaseService {
    private final LegalCaseRepository legalCaseRepository;

    @Transactional
    public LegalCaseEntity create(LegalCaseCreateRequest request, String createdBy) {
        LegalCaseEntity entity = new LegalCaseEntity();
        entity.setCaseNo(generateCaseNo());
        entity.setTitle(request.getTitle());
        entity.setClientPartnerId(request.getClientPartnerId());
        entity.setCaseType(request.getCaseType());
        entity.setCaseCategory(request.getCaseCategory());
        entity.setDescription(request.getDescription());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        entity.setAssignedTo(request.getAssignedTo());
        entity.setOpenedDate(request.getOpenedDate() != null ? request.getOpenedDate() : LocalDate.now());
        entity.setCourtName(request.getCourtName());
        entity.setCourtCaseNo(request.getCourtCaseNo());
        entity.setForumType(request.getForumType());
        if (request.getBillingType() != null) entity.setBillingType(request.getBillingType());
        entity.setHourlyRateCents(request.getHourlyRateCents());
        entity.setFixedFeeCents(request.getFixedFeeCents());
        if (request.getBillable() != null) entity.setBillable(request.getBillable());
        entity.setStatus(LegalCaseStatus.OPEN);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        return legalCaseRepository.save(entity);
    }

    public List<LegalCaseEntity> findAll() { return legalCaseRepository.findAll(); }
    public LegalCaseEntity findById(String id) { return legalCaseRepository.findById(id).orElseThrow(() -> new RuntimeException("Case not found")); }
    public List<LegalCaseEntity> findByClientPartnerId(String clientPartnerId) { return legalCaseRepository.findByClientPartnerIdOrderByOpenedDateDesc(clientPartnerId); }
    public List<LegalCaseEntity> findByAssignedTo(String assignedTo) { return legalCaseRepository.findByAssignedToOrderByOpenedDateDesc(assignedTo); }

    @Transactional
    public LegalCaseEntity update(String id, LegalCaseUpdateRequest request, String updatedBy) {
        LegalCaseEntity entity = findById(id);
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        entity.setCaseCategory(request.getCaseCategory());
        entity.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
            if (request.getStatus() == LegalCaseStatus.CLOSED) entity.setClosedDate(LocalDate.now());
        }
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        entity.setAssignedTo(request.getAssignedTo());
        entity.setCourtName(request.getCourtName());
        entity.setCourtCaseNo(request.getCourtCaseNo());
        entity.setForumType(request.getForumType());
        entity.setNextAppearanceDate(request.getNextAppearanceDate());
        if (request.getBillingType() != null) entity.setBillingType(request.getBillingType());
        entity.setHourlyRateCents(request.getHourlyRateCents());
        entity.setFixedFeeCents(request.getFixedFeeCents());
        if (request.getBillable() != null) entity.setBillable(request.getBillable());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(updatedBy);
        return legalCaseRepository.save(entity);
    }

    @Transactional
    public LegalCaseEntity closeCase(String id, String updatedBy) {
        LegalCaseEntity entity = findById(id);
        entity.setStatus(LegalCaseStatus.CLOSED);
        entity.setClosedDate(LocalDate.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(updatedBy);
        return legalCaseRepository.save(entity);
    }

    private String generateCaseNo() {
        String prefix = "CASE-" + LocalDate.now().getYear() + "-";
        long count = legalCaseRepository.count() + 1;
        return prefix + String.format("%06d", count);
    }
}
