package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CaseEventCreateRequest;
import za.co.mawa.bes.entity.v2.CaseEventEntity;
import za.co.mawa.bes.service.v2.CaseEventService;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CaseEventControllerV2 {
    private final CaseEventService caseEventService;
    @PostMapping("/{caseId}/events") public CaseEventEntity create(@PathVariable String caseId, @RequestBody CaseEventCreateRequest request) { return caseEventService.create(caseId, request, null); }
    @GetMapping("/{caseId}/events") public List<CaseEventEntity> findByCase(@PathVariable String caseId) { return caseEventService.findByCaseId(caseId); }
}
