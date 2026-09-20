package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.dto.v2.payapp.DeviceUserPaymentProfileResponse;
import za.co.mawa.bes.service.UserService;
import za.co.mawa.bes.service.v2.CardTerminalService;

@RestController
@RequiredArgsConstructor
@RequestMapping("v2/device-sync/user-payment-profile")
public class DeviceUserPaymentProfileControllerV2 {
    private final UserService userService;
    private final CardTerminalService cardTerminalService;

    @GetMapping("/{userId}")
    public DeviceUserPaymentProfileResponse get(@PathVariable String userId) throws Exception {
        UserDto user = userService.getUserById(userId);
        return new DeviceUserPaymentProfileResponse(
                user.getId(),
                user.getUsername(),
                cardTerminalService.activeAssignmentOrNull(user.getCardTerminalId())
        );
    }
}
