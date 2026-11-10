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
import za.co.mawa.bes.service.UserAccessService;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/payment-batches")
public class PaymentBatchControllerV2 {

    private final MembershipPremiumPaymentService membershipPremiumPaymentService;
    private final PremiumPaymentEditService premiumPaymentEditService;
    private final UserAccessService userAccessService;

    @GetMapping("/membership-premiums/permissions")
    public Map<String, Boolean> permissions(@RequestHeader(value = "X-Role", required = false) String role) {
        return Map.of(
                "create", userAccessService.hasPermission("premium-payment-create", role),
                "edit", userAccessService.hasPermission("premium-payment-edit", role),
                "cancel", userAccessService.hasPermission("premium-payment-cancel", role),
                "transfer", userAccessService.hasPermission("premium-payment-transfer", role));
    }

    @PostMapping("/membership-premiums")
    public PaymentBatchResponseDto createMembershipPremiumPayment(
            @RequestBody MembershipPremiumPaymentCreateRequest request,
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        userAccessService.assertPermission("premium-payment-create", role);
        return membershipPremiumPaymentService.createPayment(request);
    }
    @PostMapping("/membership-premiums/manual-receipts")
    public PaymentBatchResponseDto captureManualMembershipPremiumReceipt(
            @RequestBody ManualPremiumReceiptCaptureRequest request,
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        userAccessService.assertPermission("premium-payment-create", role);
        return membershipPremiumPaymentService.captureManualReceipt(request);
    }

    @PostMapping("/{paymentBatchId}/deletion-request")
    public ApprovalRequestResponse requestPremiumPaymentDeletion(
            @PathVariable String paymentBatchId,
            @RequestBody PremiumPaymentDeletionRequest request,
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        userAccessService.assertPermission("premium-payment-cancel", role);
        return membershipPremiumPaymentService.requestDeletion(paymentBatchId, request);
    }

    @PostMapping("/{paymentBatchId}/cancellation-request")
    public ApprovalRequestResponse requestPremiumPaymentCancellation(
            @PathVariable String paymentBatchId,
            @RequestBody PremiumPaymentDeletionRequest request,
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        userAccessService.assertPermission("premium-payment-cancel", role);
        return membershipPremiumPaymentService.requestDeletion(paymentBatchId, request);
    }

    @PostMapping("/{paymentBatchId}/edit-request")
    public ApprovalRequestResponse requestPremiumPaymentEdit(
            @PathVariable String paymentBatchId,
            @RequestBody PremiumPaymentEditRequest request,
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        userAccessService.assertPermission("premium-payment-edit", role);
        return premiumPaymentEditService.requestEdit(paymentBatchId, request);
    }

    @PostMapping("/{paymentBatchId}/transfer")
    public PaymentBatchResponseDto transferManualPremiumPayment(
            @PathVariable String paymentBatchId,
            @RequestBody PremiumPaymentTransferRequest request,
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        userAccessService.assertPermission("premium-payment-transfer", role);
        return membershipPremiumPaymentService.transferManualPayment(paymentBatchId, request);
    }

    @GetMapping("/{paymentBatchId}/deletion-status")
    public PremiumPaymentDeletionStatusResponse getPremiumPaymentDeletionStatus(
            @PathVariable String paymentBatchId
    ) {
        return membershipPremiumPaymentService.deletionStatus(paymentBatchId);
    }

    @GetMapping("/{paymentBatchId}/cancellation-status")
    public PremiumPaymentDeletionStatusResponse getPremiumPaymentCancellationStatus(
            @PathVariable String paymentBatchId
    ) {
        return membershipPremiumPaymentService.deletionStatus(paymentBatchId);
    }

}
