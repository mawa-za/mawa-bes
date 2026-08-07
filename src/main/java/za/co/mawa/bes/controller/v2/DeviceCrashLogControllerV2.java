package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.devicecrash.DeviceCrashLogDto;
import za.co.mawa.bes.dto.v2.devicecrash.DeviceCrashLogRequest;
import za.co.mawa.bes.service.DeviceCrashLogService;
import za.co.mawa.bes.service.UserAccessService;

import java.security.Principal;

@RestController
@RequestMapping("v2/device-crash-logs")
@RequiredArgsConstructor
public class DeviceCrashLogControllerV2 {
    private static final String SYSTEM_CONFIGURATION_WORKCENTRE = "system-configuration";

    private final DeviceCrashLogService service;
    private final UserAccessService userAccessService;

    @PostMapping
    public ResponseEntity<DeviceCrashLogDto> submit(
            @RequestBody DeviceCrashLogRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.submit(request, user(userId, principal)));
    }

    @GetMapping
    public Page<DeviceCrashLogDto> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestHeader(value = "X-Role", required = false) String selectedRole
    ) {
        userAccessService.assertWorkcentreAccess(SYSTEM_CONFIGURATION_WORKCENTRE, selectedRole);
        return service.list(search, page, size);
    }

    @GetMapping("/{logId}")
    public DeviceCrashLogDto get(
            @PathVariable String logId,
            @RequestHeader(value = "X-Role", required = false) String selectedRole
    ) {
        userAccessService.assertWorkcentreAccess(SYSTEM_CONFIGURATION_WORKCENTRE, selectedRole);
        return service.get(logId);
    }

    private String user(String header, Principal principal) {
        if (header != null && !header.isBlank()) return header.trim();
        return principal == null ? "UNKNOWN" : principal.getName();
    }
}
