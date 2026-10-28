package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CaseEventCreateRequest;
import za.co.mawa.bes.dto.v2.CaseEventStatusUpdateRequest;
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

    public List<CaseEventEntity> findByCaseId(String caseId) {
        return caseEventRepository.findByCaseIdOrderByStartAtAsc(caseId);
    }

    public List<CaseEventEntity> findUpcoming(LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = from != null ? from : LocalDateTime.now();
        LocalDateTime end = to != null ? to : start.plusDays(14);
        return caseEventRepository.findByStatusAndStartAtBetweenOrderByStartAtAsc(
                CaseEventStatus.SCHEDULED,
                start,
                end
        );
    }

    @Transactional
    public CaseEventEntity update(String eventId, CaseEventCreateRequest request) {
        CaseEventEntity entity = caseEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Case event not found"));
        if (request.getEventType() == null || request.getTitle() == null
                || request.getTitle().isBlank() || request.getStartAt() == null) {
            throw new IllegalArgumentException("Event type, title and start date are required");
        }
        entity.setEventType(request.getEventType());
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setStartAt(request.getStartAt());
        entity.setEndAt(request.getEndAt());
        entity.setLocation(request.getLocation());
        entity.setReminderAt(request.getReminderAt());
        entity.setUpdatedAt(LocalDateTime.now());
        return caseEventRepository.save(entity);
    }

    @Transactional
    public CaseEventEntity updateStatus(String eventId, CaseEventStatusUpdateRequest request) {
        CaseEventEntity entity = caseEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Case event not found"));

        entity.setStatus(request.getStatus());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(request.getUpdatedBy());

        return caseEventRepository.save(entity);
    }
}
