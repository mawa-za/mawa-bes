package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.LegalCaseCreateRequest;
import za.co.mawa.bes.dto.v2.LegalCaseUpdateRequest;
import za.co.mawa.bes.entity.v2.LegalCaseEntity;
import za.co.mawa.bes.service.v2.LegalCaseService;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class LegalCaseControllerV2 {
    private final LegalCaseService legalCaseService;
    @PostMapping public LegalCaseEntity create(@RequestBody LegalCaseCreateRequest request) { return legalCaseService.create(request, request.getAssignedTo()); }
    @GetMapping public List<LegalCaseEntity> findAll(@RequestParam(required = false) String clientPartnerId, @RequestParam(required = false) String assignedTo) {
        if (clientPartnerId != null) return legalCaseService.findByClientPartnerId(clientPartnerId);
        if (assignedTo != null) return legalCaseService.findByAssignedTo(assignedTo);
        return legalCaseService.findAll();
    }
    @GetMapping("/{caseId}") public LegalCaseEntity findById(@PathVariable String caseId) { return legalCaseService.findById(caseId); }
    @PutMapping("/{caseId}") public LegalCaseEntity update(@PathVariable String caseId, @RequestBody LegalCaseUpdateRequest request) { return legalCaseService.update(caseId, request, request.getAssignedTo()); }
    @PostMapping("/{caseId}/close") public LegalCaseEntity closeCase(@PathVariable String caseId) { return legalCaseService.closeCase(caseId, null); }
}
