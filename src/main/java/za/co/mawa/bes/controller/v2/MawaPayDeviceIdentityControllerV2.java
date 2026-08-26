package za.co.mawa.bes.controller.v2;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.jwt.JwtTokenUtil;
import za.co.mawa.bes.dto.v2.payapp.DeviceIdentityRequest;
import za.co.mawa.bes.dto.v2.payapp.DeviceIdentityResponse;
import za.co.mawa.bes.service.MawaPayDeviceIdentityService;

@RestController
@RequestMapping("v2/pay-app/device-identity")
@RequiredArgsConstructor
public class MawaPayDeviceIdentityControllerV2 {
    private final MawaPayDeviceIdentityService service;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/enroll")
    public ResponseEntity<DeviceIdentityResponse> enroll(@RequestBody DeviceIdentityRequest request) {
        return ResponseEntity.ok(service.enroll(request.getDeviceId()));
    }

    @PostMapping("/renew")
    public ResponseEntity<DeviceIdentityResponse> renew(@RequestHeader("Authorization") String authorization) {
        String token = authorization.substring("Bearer ".length());
        Claims claims = jwtTokenUtil.getClaimFromToken(token, value -> value);
        return ResponseEntity.ok(service.renew(
                claims.get("device_id", String.class),
                claims.get("token_version", Integer.class)));
    }
}
