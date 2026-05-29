package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.CaseInvoiceGenerateRequest;
import za.co.mawa.bes.dto.v2.CaseInvoiceGenerationResponse;
import za.co.mawa.bes.service.v2.CaseInvoiceGenerationService;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases")
public class CaseInvoiceControllerV2 {

    private final CaseInvoiceGenerationService caseInvoiceGenerationService;

    @PostMapping("/{caseId}/generate-invoice")
    public CaseInvoiceGenerationResponse generateInvoice(
            @PathVariable String caseId,
            @RequestBody CaseInvoiceGenerateRequest request
    ) {
        return caseInvoiceGenerationService.generateInvoice(caseId, request);
    }
}
