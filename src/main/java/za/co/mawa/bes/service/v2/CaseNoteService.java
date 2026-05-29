package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CaseNoteCreateRequest;
import za.co.mawa.bes.entity.v2.CaseNoteEntity;
import za.co.mawa.bes.enums.CaseNoteType;
import za.co.mawa.bes.repository.v2.CaseNoteRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseNoteService {
    private final CaseNoteRepository caseNoteRepository;
    private final LegalCaseService legalCaseService;

    @Transactional
    public CaseNoteEntity create(String caseId, CaseNoteCreateRequest request, String createdBy) {
        legalCaseService.findById(caseId);
        CaseNoteEntity entity = new CaseNoteEntity();
        entity.setCaseId(caseId);
        entity.setNoteType(request.getNoteType() != null ? request.getNoteType() : CaseNoteType.GENERAL);
        entity.setTitle(request.getTitle());
        entity.setNote(request.getNote());
        entity.setPrivateNote(request.getPrivateNote() != null ? request.getPrivateNote() : false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);
        return caseNoteRepository.save(entity);
    }
    public List<CaseNoteEntity> findByCaseId(String caseId) { return caseNoteRepository.findByCaseIdOrderByCreatedAtDesc(caseId); }
}
