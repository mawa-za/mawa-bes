package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.MembershipPremiumBulkEditRequest;
import za.co.mawa.bes.dto.v2.MembershipPremiumBulkEditResponse;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.service.v2.MembershipPremiumBulkEditService;

import java.security.Principal;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/membership-premium-bulk-edits")
public class MembershipPremiumBulkEditControllerV2 {
    private final MembershipPremiumBulkEditService bulkEditService;
    private final UserAccessService userAccessService;

    @GetMapping("/access")
    public Map<String, Boolean> access(
            @RequestHeader(value = "X-Role", required = false) String role) {
        return Map.of(
                "workcenter", userAccessService.hasWorkcentreAccess("bulk-membership-premium-edits", role),
                "edit", userAccessService.hasPermission("premium-payment-edit", role));
    }

    @PostMapping
    public MembershipPremiumBulkEditResponse submit(
            @RequestBody MembershipPremiumBulkEditRequest request,
            Principal principal,
            @RequestHeader(value = "X-Role", required = false) String role) {
        // A workcentre grant is navigation only; this is an independent sensitive action permission.
        userAccessService.assertPermission("premium-payment-edit", role);
        if (!userAccessService.hasWorkcentreAccess("bulk-membership-premium-edits", role)) {
            throw new SecurityException("Bulk Membership Premium Edits workcentre access is required");
        }
        String actor = UserContext.getCurrentUserId();
        if (actor == null || actor.isBlank()) {
            actor = principal == null ? UserContext.getCurrentUser() : principal.getName();
        }
        if (actor == null || actor.isBlank()) {
            throw new SecurityException("An authenticated user is required to request bulk premium edits");
        }
        return bulkEditService.submit(request, actor);
    }
}
