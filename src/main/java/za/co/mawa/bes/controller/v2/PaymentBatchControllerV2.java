package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
        import za.co.mawa.bes.dto.v2.MembershipPremiumPaymentCreateRequest;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.service.v2.MembershipPremiumPaymentService;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/payment-batches")
public class PaymentBatchControllerV2 {

    private final MembershipPremiumPaymentService membershipPremiumPaymentService;

    @PostMapping("/membership-premiums")
    public PaymentBatchResponseDto createMembershipPremiumPayment(
            @RequestBody MembershipPremiumPaymentCreateRequest request
    ) {
        return membershipPremiumPaymentService.createPayment(request);
    }
}