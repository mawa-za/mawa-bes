package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServicePasswordChangeRequestTest {
    @Test void blankProfilePasswordMustNotInvalidateAuthentication() {
        assertFalse(UserService.passwordChangeRequested(null));
        assertFalse(UserService.passwordChangeRequested(""));
        assertFalse(UserService.passwordChangeRequested(new String("")));
        assertFalse(UserService.passwordChangeRequested("   "));
        assertTrue(UserService.passwordChangeRequested("a-new-password"));
    }
}
