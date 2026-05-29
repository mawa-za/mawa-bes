package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CaseTaskCreateRequest;
import za.co.mawa.bes.dto.v2.CaseTaskStatusUpdateRequest;
import za.co.mawa.bes.entity.v2.CaseTaskEntity;
import za.co.mawa.bes.enums.CaseTaskStatus;
import za.co.mawa.bes.repository.v2.CaseTaskRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseTaskService {
    private final CaseTaskRepository caseTaskRepository;
    private final LegalCaseService legalCaseService;

    @Transactional
    public CaseTaskEntity create(String caseId, CaseTaskCreateRequest request, String createdBy) {
        legalCaseService.findById(caseId);
        CaseTaskEntity entity = new CaseTaskEntity();
        entity.setCaseId(caseId);
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setAssignedTo(request.getAssignedTo());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        entity.setDueDate(request.getDueDate());
        if (request.getBillable() != null) entity.setBillable(request.getBillable());
        entity.setEstimatedMinutes(request.getEstimatedMinutes());
        entity.setStatus(CaseTaskStatus.TODO);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        return caseTaskRepository.save(entity);
    }

    public List<CaseTaskEntity> findByCaseId(String caseId) { return caseTaskRepository.findByCaseIdOrderByDueDateAsc(caseId); }
    public List<CaseTaskEntity> findMyTasks(String userId) { return caseTaskRepository.findByAssignedToOrderByDueDateAsc(userId); }

    @Transactional
    public CaseTaskEntity updateStatus(String taskId, CaseTaskStatusUpdateRequest request, String updatedBy) {
        CaseTaskEntity entity = caseTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        entity.setStatus(request.getStatus());
        if (request.getStatus() == CaseTaskStatus.COMPLETED) {
            entity.setCompletedAt(LocalDateTime.now());
            entity.setCompletedBy(request.getCompletedBy() != null ? request.getCompletedBy() : updatedBy);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(updatedBy);
        return caseTaskRepository.save(entity);
    }
}
