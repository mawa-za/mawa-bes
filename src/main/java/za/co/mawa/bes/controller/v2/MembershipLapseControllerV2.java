package za.co.mawa.bes.controller.v2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.membership.lapse.MembershipLapseConfigurationDto;
import za.co.mawa.bes.dto.v2.membership.lapse.MembershipLapseRunResultDto;
import za.co.mawa.bes.service.v2.MembershipLapseService;

import java.security.Principal;

@RestController
@RequestMapping("/v2/membership-lapse")
public class MembershipLapseControllerV2 {

    private final MembershipLapseService service;

    public MembershipLapseControllerV2(MembershipLapseService service) {
        this.service = service;
    }

    @GetMapping("/configuration")
    public MembershipLapseConfigurationDto getConfiguration() {
        return service.configuration();
    }

    @PutMapping("/configuration")
    public MembershipLapseConfigurationDto saveConfiguration(
            @RequestBody MembershipLapseConfigurationDto request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Principal principal
    ) {
        return service.saveConfiguration(request, actor(userId, principal));
    }

    @PostMapping("/run-now")
    public MembershipLapseRunResultDto runNow(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Principal principal
    ) {
        return service.runNow(actor(userId, principal));
    }

    private String actor(String userId, Principal principal) {
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        if (UserContext.getCurrentUserId() != null && !UserContext.getCurrentUserId().isBlank()) {
            return UserContext.getCurrentUserId();
        }
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        if (UserContext.getCurrentUser() != null && !UserContext.getCurrentUser().isBlank()) {
            return UserContext.getCurrentUser();
        }
        return "SYSTEM";
    }
}
