package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.MembershipPremiumPaymentCreateRequest;
import za.co.mawa.bes.dto.v2.ManualPremiumReceiptCaptureRequest;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.PremiumPaymentDeletionRequest;
import za.co.mawa.bes.dto.v2.PremiumPaymentEditRequest;
import za.co.mawa.bes.dto.v2.PremiumPaymentDeletionStatusResponse;
import za.co.mawa.bes.dto.v2.PremiumPaymentTransferRequest;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.service.v2.MembershipPremiumPaymentService;
import za.co.mawa.bes.service.v2.PremiumPaymentEditService;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/payment-batches")
public class PaymentBatchControllerV2 {

    private final MembershipPremiumPaymentService membershipPremiumPaymentService;
    private final PremiumPaymentEditService premiumPaymentEditService;

    @PostMapping("/membership-premiums")
    public PaymentBatchResponseDto createMembershipPremiumPayment(
            @RequestBody MembershipPremiumPaymentCreateRequest request
    ) {
        return membershipPremiumPaymentService.createPayment(request);
    }
    @PostMapping("/membership-premiums/manual-receipts")
    public PaymentBatchResponseDto captureManualMembershipPremiumReceipt(
            @RequestBody ManualPremiumReceiptCaptureRequest request
    ) {
        return membershipPremiumPaymentService.captureManualReceipt(request);
    }

    @PostMapping("/{paymentBatchId}/deletion-request")
    public ApprovalRequestResponse requestPremiumPaymentDeletion(
            @PathVariable String paymentBatchId,
            @RequestBody PremiumPaymentDeletionRequest request
    ) {
        return membershipPremiumPaymentService.requestDeletion(paymentBatchId, request);
    }

    @PostMapping("/{paymentBatchId}/edit-request")
    public ApprovalRequestResponse requestPremiumPaymentEdit(
            @PathVariable String paymentBatchId,
            @RequestBody PremiumPaymentEditRequest request
    ) {
        return premiumPaymentEditService.requestEdit(paymentBatchId, request);
    }

    @PostMapping("/{paymentBatchId}/transfer")
    public PaymentBatchResponseDto transferManualPremiumPayment(
            @PathVariable String paymentBatchId,
            @RequestBody PremiumPaymentTransferRequest request
    ) {
        return membershipPremiumPaymentService.transferManualPayment(paymentBatchId, request);
    }

    @GetMapping("/{paymentBatchId}/deletion-status")
    public PremiumPaymentDeletionStatusResponse getPremiumPaymentDeletionStatus(
            @PathVariable String paymentBatchId
    ) {
        return membershipPremiumPaymentService.deletionStatus(paymentBatchId);
    }

}
