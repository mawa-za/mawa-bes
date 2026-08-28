package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.membership.change.*;
import za.co.mawa.bes.service.v2.MembershipChangeService;
import java.security.Principal;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("v2/membership-changes")
@RequiredArgsConstructor
public class MembershipChangeControllerV2 {
    private final MembershipChangeService service;

    @GetMapping("/configuration")
    public MembershipChangeConfigurationDto getConfiguration(){ return service.getConfiguration(); }

    @PutMapping("/configuration")
    public MembershipChangeConfigurationDto updateConfiguration(@RequestBody MembershipChangeConfigurationDto request, Principal principal){
        return service.updateConfiguration(request, actor(principal));
    }

    @PostMapping("/{membershipId}/transfer")
    public MembershipChangeResponse requestTransfer(@PathVariable String membershipId, @RequestBody MembershipTransferRequest request, Principal principal){
        return service.requestTransfer(membershipId, request, actor(principal));
    }

    @PostMapping("/{membershipId}/plan")
    public MembershipChangeResponse requestPlanChange(@PathVariable String membershipId, @RequestBody MembershipPlanChangeRequest request, Principal principal){
        return service.requestPlanChange(membershipId, request, actor(principal));
    }

    @PostMapping("/{membershipId}/premium-amount")
    public MembershipChangeResponse requestPremiumAmountChange(@PathVariable String membershipId, @RequestBody MembershipPremiumAmountChangeRequest request, Principal principal){
        return service.requestPremiumAmountChange(membershipId, request, actor(principal));
    }

    @PostMapping("/{membershipId}/merge")
    public MembershipChangeResponse requestMerge(@PathVariable String membershipId, @RequestBody MembershipMergeRequest request, Principal principal){
        return service.requestMerge(membershipId, request, actor(principal));
    }

    @GetMapping("/{membershipId}")
    public List<MembershipChangeResponse> list(@PathVariable String membershipId){ return service.listChanges(membershipId); }

    @GetMapping("/{membershipId}/audit")
    public List<MembershipChangeAuditResponse> audit(@PathVariable String membershipId){ return service.listAudit(membershipId); }

    private String actor(Principal principal){
        if (UserContext.getCurrentUserId() != null && !UserContext.getCurrentUserId().isBlank()) return UserContext.getCurrentUserId();
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) return principal.getName();
        if (UserContext.getCurrentUser() != null && !UserContext.getCurrentUser().isBlank()) return UserContext.getCurrentUser();
        return "SYSTEM";
    }
}
