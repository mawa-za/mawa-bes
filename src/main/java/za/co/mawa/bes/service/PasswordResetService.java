package za.co.mawa.bes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.security.PasswordResetTokenEntity;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.repository.security.PasswordResetTokenRepository;
import za.co.mawa.bes.utils.PasswordStatus;
import za.co.mawa.bes.utils.UserStatus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EncryptionService encryptionService;
    private final SettingService settingService;

    @Value("${mawa.encryption.secret:${jwt.secret}}")
    private String encryptionSecret;

    @Value("${mawa.password-reset.expiration-ms:1800000}")
    private long resetTokenExpirationMs;

    @Value("${mawa.password-reset.rate-limit.email-per-hour:5}")
    private long emailRequestsPerHour;

    @Value("${mawa.password-reset.rate-limit.ip-per-hour:20}")
    private long ipRequestsPerHour;

    @Value("${mawa.password-reset.minimum-password-length:8}")
    private int minimumPasswordLength;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            EncryptionService encryptionService,
            SettingService settingService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.encryptionService = encryptionService;
        this.settingService = settingService;
    }

    /**
     * Creates and emails an opaque, single-use reset token. This method deliberately
     * returns no user-existence information to its caller.
     */
    @Transactional
    public void requestReset(String email, String requestIp, String userAgent) {
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.getByEmailIgnoreCase(normalizedEmail);
        if (user == null || !UserStatus.ACTIVE.equalsIgnoreCase(user.getStatus())) {
            return;
        }

        Date now = new Date();
        Date rateWindowStart = new Date(now.getTime() - 3_600_000L);
        if (tokenRepository.countRecentForEmail(normalizedEmail, rateWindowStart) >= emailRequestsPerHour) {
            log.warn("Password reset email rate limit reached for tenant {}", TenantContext.getCurrentTenant());
            return;
        }
        if (StringUtils.hasText(requestIp)
                && tokenRepository.countRecentForIp(requestIp, rateWindowStart) >= ipRequestsPerHour) {
            log.warn("Password reset IP rate limit reached for tenant {}", TenantContext.getCurrentTenant());
            return;
        }

        tokenRepository.consumeActiveTokensForUser(user.getId(), now);

        String rawToken = generateOpaqueToken();
        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(hashToken(rawToken))
                .requestedEmail(normalizedEmail)
                .requestedAt(now)
                .expiresAt(new Date(now.getTime() + resetTokenExpirationMs))
                .requestIp(trimToLength(requestIp, 100))
                .userAgent(trimToLength(userAgent, 500))
                .build();
        tokenRepository.saveAndFlush(resetToken);

        try {
            EmailDto emailDto = new EmailDto();
            emailDto.setTo(user.getEmail());
            emailDto.setSubject("Reset Password");
            emailDto.setTemplate("reset-password");
            emailDto.setProperties(List.of(new PropertyDto("resetLink", buildResetLink(rawToken))));
            emailService.send(emailDto);
        } catch (RuntimeException exception) {
            resetToken.setConsumedAt(new Date());
            tokenRepository.save(resetToken);
            log.error("Unable to send password-reset email for tenant {}", TenantContext.getCurrentTenant(), exception);
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validateNewPassword(newPassword);
        if (!StringUtils.hasText(rawToken)) {
            throw new IllegalArgumentException("Reset token is invalid or expired");
        }

        PasswordResetTokenEntity resetToken = tokenRepository
                .findByTokenHashForUpdate(hashToken(rawToken.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Reset token is invalid or expired"));

        Date now = new Date();
        if (resetToken.getConsumedAt() != null || resetToken.getExpiresAt().before(now)) {
            throw new IllegalArgumentException("Reset token is invalid or expired");
        }

        UserEntity user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Reset token is invalid or expired"));
        if (!UserStatus.ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("Reset token is invalid or expired");
        }

        user.setPassword(encryptionService.encrypt(newPassword, encryptionSecret).getBytes(StandardCharsets.UTF_8));
        user.setPasswordStatus(PasswordStatus.PRODUCTIVE);
        user.setPasswordChangedAt(now);
        userRepository.save(user);

        // Consumes this token and any other still-active token for the same user.
        tokenRepository.consumeActiveTokensForUser(user.getId(), now);
    }

    public int getMinimumPasswordLength() {
        return minimumPasswordLength;
    }

    private void validateNewPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < minimumPasswordLength) {
            throw new IllegalArgumentException(
                    "Password must be at least " + minimumPasswordLength + " characters"
            );
        }
    }

    private String buildResetLink(String rawToken) {
        String tenantUrl = settingService.getSetting("ACCESS-URL", "TENANT");
        if (!StringUtils.hasText(tenantUrl)) {
            tenantUrl = TenantContext.getCurrentTenantURL();
        }
        if (!StringUtils.hasText(tenantUrl)) {
            throw new IllegalStateException("Tenant access URL is not configured");
        }

        String baseUrl = tenantUrl.trim();
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://" + baseUrl;
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/#/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format(Locale.ROOT, "%02x", value));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to secure password-reset token", exception);
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email address is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
