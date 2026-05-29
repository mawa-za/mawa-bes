package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CaseBillingSummaryResponse;
import za.co.mawa.bes.service.v2.CaseBillingService;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CaseBillingControllerV2 {
    private final CaseBillingService caseBillingService;
    @GetMapping("/{caseId}/billing-summary") public CaseBillingSummaryResponse getBillingSummary(@PathVariable String caseId) { return caseBillingService.getBillingSummary(caseId); }
    @PostMapping("/{caseId}/recalculate-billing") public void recalculate(@PathVariable String caseId) { caseBillingService.recalculateCaseTotals(caseId); }
}
