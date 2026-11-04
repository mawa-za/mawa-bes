package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.MembershipPremiumPaymentSyncOfflineRequest;
import za.co.mawa.bes.dto.v2.GroupSocietyPaymentSyncOfflineRequest;
import za.co.mawa.bes.dto.v2.PaymentSyncOfflineResponseDto;
import za.co.mawa.bes.dto.v2.payapp.CashupRequest;
import za.co.mawa.bes.dto.v2.payapp.CashupResponse;
import za.co.mawa.bes.service.v2.MembershipPremiumSyncOfflineService;
import za.co.mawa.bes.service.v2.GroupSocietyPaymentSyncOfflineService;
import za.co.mawa.bes.service.v2.CashupService;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.configuration.context.UserContext;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/sync")
public class OfflineSyncControllerV2 {

    private final MembershipPremiumSyncOfflineService membershipPremiumSyncOfflineService;

    private final GroupSocietyPaymentSyncOfflineService groupSocietyPaymentSyncOfflineService;

    private final CashupService cashupService;
    private final UserAccessService userAccessService;

    @PostMapping("/payment-batches/membership-premiums")
    public PaymentSyncOfflineResponseDto syncMembershipPremiumPayment(
            @RequestBody MembershipPremiumPaymentSyncOfflineRequest request,
            @RequestHeader(value = "X-Role", required = false) String role
    ) {
        // Enrolled device credentials have their own revocable sync authorization.
        // Interactive users must not bypass collection permissions via this route.
        if (!UserContext.isBackgroundSession()) {
            userAccessService.assertWorkcentreAccess("premium-payment-create", role);
        }
        return membershipPremiumSyncOfflineService.sync(request);
    }

    @PostMapping("/payment-batches/group-society")
    public PaymentSyncOfflineResponseDto syncGroupSocietyPayment(
            @RequestBody GroupSocietyPaymentSyncOfflineRequest request
    ) {
        return groupSocietyPaymentSyncOfflineService.sync(request);
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
