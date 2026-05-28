package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CaseTimeEntryCreateRequest;
import za.co.mawa.bes.entity.v2.CaseTimeEntryEntity;
import za.co.mawa.bes.service.v2.CaseTimeEntryService;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CaseTimeEntryControllerV2 {
    private final CaseTimeEntryService caseTimeEntryService;
    @PostMapping("/{caseId}/time-entries") public CaseTimeEntryEntity create(@PathVariable String caseId, @RequestBody CaseTimeEntryCreateRequest request) { return caseTimeEntryService.create(caseId, request, request.getUserId()); }
    @GetMapping("/{caseId}/time-entries") public List<CaseTimeEntryEntity> findByCase(@PathVariable String caseId) { return caseTimeEntryService.findByCaseId(caseId); }
}
