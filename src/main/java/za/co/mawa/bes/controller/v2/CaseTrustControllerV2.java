package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.v2.CaseTrustTransactionEntity;
import za.co.mawa.bes.service.v2.CaseTrustService;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/cases/{caseId}/trust")
public class CaseTrustControllerV2 {

    private final CaseTrustService caseTrustService;

    @GetMapping("/balance")
    public CaseTrustBalanceResponse getBalance(@PathVariable String caseId) {
        return caseTrustService.getBalance(caseId);
    }

    @GetMapping("/transactions")
    public List<CaseTrustTransactionEntity> getTransactions(@PathVariable String caseId) {
        return caseTrustService.getTransactions(caseId);
    }

    @PostMapping("/receipts")
    public CaseTrustTransactionEntity receiveTrustFunds(
            @PathVariable String caseId,
            @RequestBody CaseTrustReceiptRequest request
    ) {
        return caseTrustService.receiveTrustFunds(caseId, request);
    }

    @PostMapping("/transfers/business")
    public CaseTrustTransactionEntity transferToBusiness(
            @PathVariable String caseId,
            @RequestBody CaseTrustBusinessTransferRequest request
    ) {
        return caseTrustService.transferToBusiness(caseId, request);
    }

    @PostMapping("/refunds")
    public CaseTrustTransactionEntity refundClient(
            @PathVariable String caseId,
            @RequestBody CaseTrustRefundRequest request
    ) {
        return caseTrustService.refundClient(caseId, request);
    }

    @PostMapping("/third-party-payments")
    public CaseTrustTransactionEntity payThirdParty(
            @PathVariable String caseId,
            @RequestBody CaseTrustThirdPartyPaymentRequest request
    ) {
        return caseTrustService.payThirdParty(caseId, request);
    }

    @PostMapping("/transactions/{transactionId}/reverse")
    public CaseTrustTransactionEntity reverseTransaction(
            @PathVariable String caseId,
            @PathVariable String transactionId,
            @RequestBody CaseTrustReverseTransactionRequest request
    ) {
        return caseTrustService.reverseTransaction(caseId, transactionId, request);
    }
}
