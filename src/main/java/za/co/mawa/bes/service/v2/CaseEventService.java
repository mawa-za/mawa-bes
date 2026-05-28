package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CaseEventCreateRequest;
import za.co.mawa.bes.entity.v2.CaseEventEntity;
import za.co.mawa.bes.enums.CaseEventStatus;
import za.co.mawa.bes.repository.v2.CaseEventRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseEventService {
    private final CaseEventRepository caseEventRepository;
    private final LegalCaseService legalCaseService;

    @Transactional
    public CaseEventEntity create(String caseId, CaseEventCreateRequest request, String createdBy) {
        legalCaseService.findById(caseId);
        CaseEventEntity entity = new CaseEventEntity();
        entity.setCaseId(caseId);
        entity.setEventType(request.getEventType());
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setStartAt(request.getStartAt());
        entity.setEndAt(request.getEndAt());
        entity.setLocation(request.getLocation());
        entity.setReminderAt(request.getReminderAt());
        entity.setStatus(CaseEventStatus.SCHEDULED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        return caseEventRepository.save(entity);
    }
    public List<CaseEventEntity> findByCaseId(String caseId) { return caseEventRepository.findByCaseIdOrderByStartAtAsc(caseId); }
}
