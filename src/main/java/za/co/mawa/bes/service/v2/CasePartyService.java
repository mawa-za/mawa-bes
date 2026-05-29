package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CasePartyCreateRequest;
import za.co.mawa.bes.entity.v2.CasePartyEntity;
import za.co.mawa.bes.enums.CasePartyType;
import za.co.mawa.bes.repository.v2.CasePartyRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CasePartyService {

    private final CasePartyRepository casePartyRepository;
    private final LegalCaseService legalCaseService;

    @Transactional
    public CasePartyEntity create(String caseId, CasePartyCreateRequest request, String createdBy) {
        legalCaseService.findById(caseId);

        CasePartyEntity entity = new CasePartyEntity();
        entity.setCaseId(caseId);
        entity.setPartnerId(request.getPartnerId());
        entity.setPartyName(request.getPartyName());
        entity.setPartyType(request.getPartyType());
        entity.setIdNumber(request.getIdNumber());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setAttorneyFirm(request.getAttorneyFirm());
        entity.setAttorneyName(request.getAttorneyName());
        entity.setNotes(request.getNotes());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(createdBy);

        return casePartyRepository.save(entity);
    }

    public List<CasePartyEntity> findByCaseId(String caseId, CasePartyType partyType) {
        if (partyType != null) {
            return casePartyRepository.findByCaseIdAndPartyTypeOrderByPartyNameAsc(caseId, partyType);
        }
        return casePartyRepository.findByCaseIdOrderByPartyNameAsc(caseId);
    }

    @Transactional
    public void delete(String partyId) {
        casePartyRepository.deleteById(partyId);
    }
}
