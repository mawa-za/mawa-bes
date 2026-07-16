package za.co.mawa.bes.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.security.PasswordResetTokenEntity;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.repository.security.PasswordResetTokenRepository;
import za.co.mawa.bes.utils.UserStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private SettingService settingService;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                tokenRepository,
                userRepository,
                emailService,
                encryptionService,
                settingService
        );
        ReflectionTestUtils.setField(service, "encryptionSecret", "test-secret");
        ReflectionTestUtils.setField(service, "resetTokenExpirationMs", 1_800_000L);
        ReflectionTestUtils.setField(service, "emailRequestsPerHour", 5L);
        ReflectionTestUtils.setField(service, "ipRequestsPerHour", 20L);
        ReflectionTestUtils.setField(service, "minimumPasswordLength", 8);
        TenantContext.setCurrentTenant("tenant-1");
        TenantContext.setCurrentTenantURL("dev.app.mawa.co.za");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void requestResetStoresOnlyHashAndEmailsOpaqueToken() throws Exception {
        UserEntity user = UserEntity.builder()
                .id("user-1")
                .username("test.user")
                .email("Test.User@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.getByEmailIgnoreCase("test.user@example.com")).thenReturn(user);
        when(tokenRepository.countRecentForEmail(anyString(), any(Date.class))).thenReturn(0L);
        when(tokenRepository.countRecentForIp(anyString(), any(Date.class))).thenReturn(0L);
        when(settingService.getSetting("ACCESS-URL", "TENANT")).thenReturn("dev.app.mawa.co.za");

        service.requestReset(" Test.User@example.com ", "127.0.0.1", "JUnit");

        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository).saveAndFlush(tokenCaptor.capture());
        PasswordResetTokenEntity storedToken = tokenCaptor.getValue();
        assertEquals(64, storedToken.getTokenHash().length());
        assertEquals("test.user@example.com", storedToken.getRequestedEmail());
        assertNotNull(storedToken.getExpiresAt());

        ArgumentCaptor<EmailDto> emailCaptor = ArgumentCaptor.forClass(EmailDto.class);
        verify(emailService).send(emailCaptor.capture());
        String resetLink = emailCaptor.getValue().getProperties().get(0).getValue();
        assertTrue(resetLink.startsWith("https://dev.app.mawa.co.za/#/reset-password?token="));
        String rawToken = resetLink.substring(resetLink.indexOf("token=") + 6);
        assertEquals(43, rawToken.length());
        assertEquals(
                HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256")
                                .digest(rawToken.getBytes(StandardCharsets.UTF_8))
                ),
                storedToken.getTokenHash()
        );
        verify(tokenRepository).consumeActiveTokensForUser("user-1", storedToken.getRequestedAt());
    }

    @Test
    void requestResetDoesNotRevealOrPersistUnknownAccount() {
        when(userRepository.getByEmailIgnoreCase("missing@example.com")).thenReturn(null);

        service.requestReset("missing@example.com", "127.0.0.1", "JUnit");

        verify(tokenRepository, never()).saveAndFlush(any());
        verify(emailService, never()).send(any());
    }

    @Test
    void resetPasswordConsumesTokenAndInvalidatesExistingSessions() {
        Date now = new Date();
        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .id("token-1")
                .userId("user-1")
                .tokenHash("hash")
                .requestedEmail("test@example.com")
                .requestedAt(now)
                .expiresAt(new Date(now.getTime() + 60_000L))
                .build();
        UserEntity user = UserEntity.builder()
                .id("user-1")
                .username("test.user")
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(encryptionService.encrypt("StrongPass1", "test-secret")).thenReturn("encrypted-password");

        service.resetPassword("raw-token", "StrongPass1");

        assertArrayEquals("encrypted-password".getBytes(StandardCharsets.UTF_8), user.getPassword());
        assertNotNull(user.getPasswordChangedAt());
        verify(userRepository).save(user);
        verify(tokenRepository).consumeActiveTokensForUser("user-1", user.getPasswordChangedAt());
    }

    @Test
    void resetPasswordRejectsWeakPasswordBeforeTokenLookup() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.resetPassword("raw-token", "short")
        );
        assertEquals("Password must be at least 8 characters", exception.getMessage());
        verify(tokenRepository, never()).findByTokenHashForUpdate(anyString());
    }
}
