package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.MembershipPremiumPaymentSyncOfflineRequest;
import za.co.mawa.bes.dto.v2.PaymentSyncOfflineResponseDto;
import za.co.mawa.bes.dto.v2.payapp.CashupRequest;
import za.co.mawa.bes.dto.v2.payapp.CashupResponse;
import za.co.mawa.bes.service.v2.MembershipPremiumSyncOfflineService;
import za.co.mawa.bes.service.v2.CashupService;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/sync")
public class OfflineSyncControllerV2 {

    private final MembershipPremiumSyncOfflineService membershipPremiumSyncOfflineService;

    private final CashupService cashupService;

    @PostMapping("/payment-batches/membership-premiums")
    public PaymentSyncOfflineResponseDto syncMembershipPremiumPayment(
            @RequestBody MembershipPremiumPaymentSyncOfflineRequest request
    ) {
        return membershipPremiumSyncOfflineService.sync(request);
    }


    /**
     * Offline app submits or updates an active cashup.
     *
     * This mirrors POST /v2/cashup but keeps offline-device sync endpoints under /v2/sync.
     * It allows MawaPay to use the same sync route family as payment batches.
     */
    @PostMapping("/cashups")
    public ResponseEntity<CashupResponse> syncCashup(@RequestBody CashupRequest request) {
        return ResponseEntity.ok(cashupService.submitCashup(request));
    }

}
