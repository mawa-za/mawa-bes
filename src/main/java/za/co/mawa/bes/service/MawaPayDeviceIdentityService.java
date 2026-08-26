package za.co.mawa.bes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.configuration.jwt.JwtTokenUtil;
import za.co.mawa.bes.dto.v2.payapp.DeviceIdentityResponse;
import za.co.mawa.bes.entity.MawaPayDeviceIdentityEntity;
import za.co.mawa.bes.repository.MawaPayDeviceIdentityRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MawaPayDeviceIdentityService {
    private static final long TOKEN_LIFETIME_SECONDS = 90L * 24 * 60 * 60;
    private final MawaPayDeviceIdentityRepository repository;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional
    public DeviceIdentityResponse enroll(String deviceId) {
        String normalized = required(deviceId);
        MawaPayDeviceIdentityEntity identity = repository.findByDeviceId(normalized)
                .orElseGet(() -> newIdentity(normalized));
        identity.setStatus("ACTIVE");
        identity.setRevokedAt(null);
        identity.setEnrolledBy(UserContext.getCurrentUser());
        identity.setLastSeenAt(LocalDateTime.now());
        identity = repository.save(identity);
        return response(identity);
    }

    @Transactional
    public DeviceIdentityResponse renew(String deviceId, int tokenVersion) {
        MawaPayDeviceIdentityEntity identity = requireActive(deviceId, tokenVersion);
        identity.setLastSeenAt(LocalDateTime.now());
        return response(repository.save(identity));
    }

    @Transactional(readOnly = true)
    public MawaPayDeviceIdentityEntity requireActive(String deviceId, int tokenVersion) {
        MawaPayDeviceIdentityEntity identity = repository.findByDeviceId(required(deviceId))
                .orElseThrow(() -> new IllegalArgumentException("Device is not enrolled"));
        if (!"ACTIVE".equals(identity.getStatus()) || identity.getTokenVersion() != tokenVersion) {
            throw new IllegalArgumentException("Device sync identity is revoked");
        }
        return identity;
    }

    private MawaPayDeviceIdentityEntity newIdentity(String deviceId) {
        MawaPayDeviceIdentityEntity identity = new MawaPayDeviceIdentityEntity();
        identity.setId(UUID.randomUUID().toString());
        identity.setDeviceId(deviceId);
        identity.setStatus("ACTIVE");
        identity.setTokenVersion(1);
        identity.setEnrolledAt(LocalDateTime.now());
        return identity;
    }

    private DeviceIdentityResponse response(MawaPayDeviceIdentityEntity identity) {
        return new DeviceIdentityResponse(identity.getDeviceId(),
                jwtTokenUtil.generateDeviceSyncToken(identity.getDeviceId(), identity.getTokenVersion()),
                TOKEN_LIFETIME_SECONDS);
    }

    private String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("deviceId is required");
        return value.trim();
    }
}
