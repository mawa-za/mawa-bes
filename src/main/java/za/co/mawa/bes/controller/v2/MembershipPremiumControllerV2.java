package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
        import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.service.v2.MembershipPremiumService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/memberships/{membershipId}/premiums")
public class MembershipPremiumControllerV2 {

    private final MembershipPremiumService membershipPremiumService;

    @GetMapping
    public List<MembershipPremiumEntity> getPremiums(
            @PathVariable String membershipId
    ) {
        return membershipPremiumService.getPremiumsForMembership(membershipId);
    }

    @GetMapping("/unpaid")
    public List<MembershipPremiumEntity> getUnpaidPremiums(
            @PathVariable String membershipId
    ) {
        return membershipPremiumService.getUnpaidPremiums(membershipId);
    }
}