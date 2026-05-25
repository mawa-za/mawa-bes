package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
        import za.co.mawa.bes.dto.v2.MembershipPremiumPaymentSyncOfflineRequest;
import za.co.mawa.bes.dto.v2.PaymentSyncOfflineResponseDto;
import za.co.mawa.bes.service.v2.MembershipPremiumSyncOfflineService;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/sync")
public class OfflineSyncControllerV2 {

    private final MembershipPremiumSyncOfflineService membershipPremiumSyncOfflineService;

    @PostMapping("/payment-batches/membership-premiums")
    public PaymentSyncOfflineResponseDto syncMembershipPremiumPayment(
            @RequestBody MembershipPremiumPaymentSyncOfflineRequest request
    ) {
        return membershipPremiumSyncOfflineService.sync(request);
    }
}