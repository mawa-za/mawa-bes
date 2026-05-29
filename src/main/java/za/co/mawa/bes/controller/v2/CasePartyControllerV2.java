package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CasePartyCreateRequest;
import za.co.mawa.bes.entity.v2.CasePartyEntity;
import za.co.mawa.bes.enums.CasePartyType;
import za.co.mawa.bes.service.v2.CasePartyService;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CasePartyControllerV2 {

    private final CasePartyService casePartyService;

    @PostMapping("/{caseId}/parties")
    public CasePartyEntity create(
            @PathVariable String caseId,
            @RequestBody CasePartyCreateRequest request
    ) {
        return casePartyService.create(caseId, request, null);
    }

    @GetMapping("/{caseId}/parties")
    public List<CasePartyEntity> findByCase(
            @PathVariable String caseId,
            @RequestParam(required = false) CasePartyType partyType
    ) {
        return casePartyService.findByCaseId(caseId, partyType);
    }

    @DeleteMapping("/parties/{partyId}")
    public void delete(@PathVariable String partyId) {
        casePartyService.delete(partyId);
    }
}
