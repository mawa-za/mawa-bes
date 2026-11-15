package za.co.mawa.bes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import za.co.mawa.bes.dto.user.ChangePasswordRequestDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.utils.PasswordStatus;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceChangePasswordTest {
    @Mock UserRepository userRepository;
    @Mock EncryptionService encryptionService;
    @InjectMocks UserService userService;

    private UserEntity existingUser;

    @BeforeEach
    void setUp() {
        userService.setEncryptionSecret("test-secret");
        existingUser = new UserEntity();
        existingUser.setUsername("cashier");
        existingUser.setPassword("ENC-current".getBytes(StandardCharsets.UTF_8));
    }

    private ChangePasswordRequestDto request(String oldPassword, String newPassword) {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    @Test
    void changesOnlyAuthenticatedUsersPasswordWhenCurrentMatches() {
        when(userRepository.getByName("cashier")).thenReturn(existingUser);
        when(encryptionService.encrypt("Current123", "test-secret")).thenReturn("ENC-current");
        when(encryptionService.encrypt("NewSecure123", "test-secret")).thenReturn("ENC-new");
        userService.changeOwnPassword("cashier", request("Current123", "NewSecure123"));
        assertEquals("ENC-new", new String(existingUser.getPassword(), StandardCharsets.UTF_8));
        assertEquals(PasswordStatus.PRODUCTIVE, existingUser.getPasswordStatus());
        assertNotNull(existingUser.getPasswordChangedAt());
        verify(userRepository).save(existingUser);
        verify(userRepository, never()).getByName("someone-else");
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutSaving() {
        when(userRepository.getByName("cashier")).thenReturn(existingUser);
        when(encryptionService.encrypt("Wrong123", "test-secret")).thenReturn("ENC-wrong");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> userService.changeOwnPassword("cashier", request("Wrong123", "NewSecure123")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(userRepository, never()).save(any());
        assertArrayEquals("ENC-current".getBytes(StandardCharsets.UTF_8), existingUser.getPassword());
    }

    @Test
    void rejectsShortNewPasswordWithoutReadingUser() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> userService.changeOwnPassword("cashier", request("Current123", "short")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsSamePasswordAndBlankOldPassword() {
        assertThrows(ResponseStatusException.class,
                () -> userService.changeOwnPassword("cashier", request("Current123", "Current123")));
        assertThrows(ResponseStatusException.class,
                () -> userService.changeOwnPassword("cashier", request("", "NewSecure123")));
        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsBlankNewPasswordWithoutReadingUser() {
        assertThrows(ResponseStatusException.class,
                () -> userService.changeOwnPassword("cashier", request("Current123", "       ")));
        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsAnonymousUser() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> userService.changeOwnPassword(null, request("Current123", "NewSecure123")));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(userRepository);
    }
}
