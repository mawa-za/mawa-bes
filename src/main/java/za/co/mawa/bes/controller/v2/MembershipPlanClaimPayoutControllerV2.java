package za.co.mawa.bes.controller.v2;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutResponseDto;
import za.co.mawa.bes.service.v2.MembershipPlanClaimPayoutService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("v2/membership/plans/{planId}/claim-payout")
@RequiredArgsConstructor
public class MembershipPlanClaimPayoutControllerV2 {

    private final MembershipPlanClaimPayoutService payoutService;

    @PostMapping
    public MembershipPlanClaimPayoutResponseDto create(
            @PathVariable String planId,
            @Valid @RequestBody MembershipPlanClaimPayoutRequestDto dto,
            Principal principal
    ) {
        return payoutService.create(planId, dto, getUsername(principal));
    }

    @PutMapping("/{payoutId}")
    public MembershipPlanClaimPayoutResponseDto update(
            @PathVariable String planId,
            @PathVariable String payoutId,
            @Valid @RequestBody MembershipPlanClaimPayoutRequestDto dto,
            Principal principal
    ) {
        return payoutService.update(planId, payoutId, dto, getUsername(principal));
    }

    @GetMapping
    public List<MembershipPlanClaimPayoutResponseDto> getActiveByPlan(
            @PathVariable String planId
    ) {
        return payoutService.getActiveByPlan(planId);
    }

    @GetMapping("/all")
    public List<MembershipPlanClaimPayoutResponseDto> getAllByPlan(
            @PathVariable String planId
    ) {
        return payoutService.getAllByPlan(planId);
    }

    @DeleteMapping("/{payoutId}")
    public void deactivate(
            @PathVariable String planId,
            @PathVariable String payoutId,
            Principal principal
    ) {
        payoutService.deactivate(planId, payoutId, getUsername(principal));
    }

    private String getUsername(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }
}
