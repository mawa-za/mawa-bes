package za.co.mawa.bes.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void accessValidationIsReturnedAsForbiddenInsteadOfServerError() {
        var response = handler.handleSecurity(new SecurityException("Administrator access required"));

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Administrator access required.", response.getBody().getMessage());
    }

    @Test
    void businessValidationIsReturnedAsBadRequestInsteadOfServerError() {
        var response = handler.handleInvalidRequest(new IllegalArgumentException("Configure an FNB debtor account"));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Configure an FNB debtor account.", response.getBody().getMessage());
    }
}
