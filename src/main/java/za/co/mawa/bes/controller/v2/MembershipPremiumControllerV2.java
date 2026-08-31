package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.MembershipPremiumResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.MembershipPremiumEditRequest;
import za.co.mawa.bes.dto.v2.MembershipPremiumRecalculationResponse;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.service.v2.MembershipPremiumService;
import za.co.mawa.bes.service.v2.MembershipPremiumEditService;

import java.security.Principal;
import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/memberships/{membershipId}/premiums")
public class MembershipPremiumControllerV2 {

    private final MembershipPremiumService membershipPremiumService;
    private final MembershipPremiumEditService membershipPremiumEditService;

    @GetMapping
    public List<MembershipPremiumResponseDto> getPremiums(
            @PathVariable String membershipId
    ) {
        return membershipPremiumService.getPremiumHistory(membershipId);
    }

    @GetMapping("/{premiumId}/receipts")
    public List<ReceiptResponseDto> getPremiumReceipts(
            @PathVariable String membershipId,
            @PathVariable String premiumId
    ) {
        return membershipPremiumService.getPremiumReceipts(membershipId, premiumId);
    }

    @PostMapping("/{premiumId}/edit-request")
    public ApprovalRequestResponse requestPremiumEdit(
            @PathVariable String membershipId,
            @PathVariable String premiumId,
            @RequestBody MembershipPremiumEditRequest request,
            Principal principal) {
        return membershipPremiumEditService.requestEdit(membershipId, premiumId, request, actor(principal));
    }

    @GetMapping("/unpaid")
    public List<MembershipPremiumEntity> getUnpaidPremiums(
            @PathVariable String membershipId
    ) {
        return membershipPremiumService.getUnpaidPremiums(membershipId);
    }

    @PostMapping("/recalculate")
    public MembershipPremiumRecalculationResponse recalculatePremiums(
            @PathVariable String membershipId,
            Principal principal
    ) {
        return membershipPremiumService.reconcileMembership(membershipId, actor(principal));
    }


    private String actor(Principal principal) {
        if (UserContext.getCurrentUserId() != null && !UserContext.getCurrentUserId().isBlank()) return UserContext.getCurrentUserId();
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) return principal.getName();
        if (UserContext.getCurrentUser() != null && !UserContext.getCurrentUser().isBlank()) return UserContext.getCurrentUser();
        return "SYSTEM";
    }
}
