package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.MembershipPremiumResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.service.v2.MembershipPremiumService;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/memberships/{membershipId}/premiums")
public class MembershipPremiumControllerV2 {

    private final MembershipPremiumService membershipPremiumService;

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

    @GetMapping("/unpaid")
    public List<MembershipPremiumEntity> getUnpaidPremiums(
            @PathVariable String membershipId
    ) {
        return membershipPremiumService.getUnpaidPremiums(membershipId);
    }
}