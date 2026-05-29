package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CaseNoteCreateRequest;
import za.co.mawa.bes.entity.v2.CaseNoteEntity;
import za.co.mawa.bes.service.v2.CaseNoteService;
import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CaseNoteControllerV2 {
    private final CaseNoteService caseNoteService;
    @PostMapping("/{caseId}/notes") public CaseNoteEntity create(@PathVariable String caseId, @RequestBody CaseNoteCreateRequest request) { return caseNoteService.create(caseId, request, null); }
    @GetMapping("/{caseId}/notes") public List<CaseNoteEntity> findByCase(@PathVariable String caseId) { return caseNoteService.findByCaseId(caseId); }
}
