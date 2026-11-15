package za.co.mawa.bes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import za.co.mawa.bes.dto.user.ChangePasswordRequestDto;
import za.co.mawa.bes.service.UserService;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChangePasswordControllerTest {
    @Test
    void endpointIsMappedAndUsesPrincipalNotRequestSuppliedUserId() throws Exception {
        var annotation = AuthenticationController.class
                .getMethod("updateOwnPassword", ChangePasswordRequestDto.class, Principal.class)
                .getAnnotation(PostMapping.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{"/v2/update-password"}, annotation.value());

        UserService service = mock(UserService.class);
        AuthenticationController controller = new AuthenticationController();
        ReflectionTestUtils.setField(controller, "userService", service);
        ChangePasswordRequestDto request = new ChangePasswordRequestDto();
        request.setOldPassword("Current123");
        request.setNewPassword("NewSecure123");
        Principal principal = () -> "cashier";
        ResponseEntity<Void> response = controller.updateOwnPassword(request, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(service).changeOwnPassword("cashier", request);
    }

    @Test
    void anonymousRequestsAreRejected() {
        UserService service = mock(UserService.class);
        AuthenticationController controller = new AuthenticationController();
        ReflectionTestUtils.setField(controller, "userService", service);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.updateOwnPassword(new ChangePasswordRequestDto(), null));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(service);
    }
}
