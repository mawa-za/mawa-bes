package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleResponseDto;
import za.co.mawa.bes.service.v2.MembershipPlanPremiumRuleService;

import java.security.Principal;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("v2/membership/plans/{planId}/premium-rule")
@RequiredArgsConstructor
public class MembershipPlanPremiumRuleControllerV2 {

    private final MembershipPlanPremiumRuleService premiumRuleService;

    @PostMapping
    public MembershipPlanPremiumRuleResponseDto create(
            @PathVariable String planId,
            @RequestBody MembershipPlanPremiumRuleRequestDto dto,
            Principal principal
    ) {
        return premiumRuleService.create(planId, dto, principal.getName());
    }

    @PutMapping("/{ruleId}")
    public MembershipPlanPremiumRuleResponseDto update(
            @PathVariable String planId,
            @PathVariable String ruleId,
            @RequestBody MembershipPlanPremiumRuleRequestDto dto,
            Principal principal
    ) {
        return premiumRuleService.update(planId, ruleId, dto, principal.getName());
    }

    @GetMapping
    public List<MembershipPlanPremiumRuleResponseDto> getByPlan(
            @PathVariable String planId
    ) {
        return premiumRuleService.getByPlan(planId);
    }

    @DeleteMapping("/{ruleId}")
    public void deactivate(
            @PathVariable String planId,
            @PathVariable String ruleId,
            Principal principal
    ) {
        premiumRuleService.deactivate(planId, ruleId, principal.getName());
    }
}