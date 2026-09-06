package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class UserTimeZoneService {
    public static final String DEFAULT_TIME_ZONE = "Africa/Harare";

    private final UserRepository userRepository;

    public ZoneId selectedZoneId() {
        UserEntity user = currentUser();
        String configured = user == null ? null : user.getTimeZone();
        try {
            return ZoneId.of(configured == null || configured.isBlank() ? DEFAULT_TIME_ZONE : configured.trim());
        } catch (RuntimeException ignored) {
            return ZoneId.of(DEFAULT_TIME_ZONE);
        }
    }

    /** Converts a timezone-less UTC database timestamp for user-facing output. */
    public LocalDateTime display(LocalDateTime utcDateTime) {
        if (utcDateTime == null) return null;
        return utcDateTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(selectedZoneId())
                .toLocalDateTime();
    }

    public LocalDateTime now() {
        return LocalDateTime.now(selectedZoneId());
    }

    private UserEntity currentUser() {
        String userId = UserContext.getCurrentUserId();
        if (userId != null && !userId.isBlank()) {
            UserEntity user = userRepository.findById(userId.trim()).orElse(null);
            if (user != null) return user;
        }
        String username = UserContext.getCurrentUser();
        return username == null || username.isBlank() ? null : userRepository.getByName(username.trim());
    }
}
