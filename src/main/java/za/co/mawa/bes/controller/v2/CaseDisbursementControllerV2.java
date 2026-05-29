package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CaseDisbursementCreateRequest;
import za.co.mawa.bes.entity.v2.CaseDisbursementEntity;
import za.co.mawa.bes.service.v2.CaseDisbursementService;
import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CaseDisbursementControllerV2 {
    private final CaseDisbursementService caseDisbursementService;
    @PostMapping("/{caseId}/disbursements") public CaseDisbursementEntity create(@PathVariable String caseId, @RequestBody CaseDisbursementCreateRequest request) { return caseDisbursementService.create(caseId, request, null); }
    @GetMapping("/{caseId}/disbursements") public List<CaseDisbursementEntity> findByCase(@PathVariable String caseId) { return caseDisbursementService.findByCaseId(caseId); }
}
