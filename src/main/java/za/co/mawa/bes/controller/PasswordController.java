package za.co.mawa.bes.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.service.PasswordResetService;

import java.util.Map;

@RestController
@CrossOrigin
public class PasswordController {

    private static final Logger log = LoggerFactory.getLogger(PasswordController.class);
    private static final String NEUTRAL_RESPONSE =
            "If the email address is registered, password reset instructions will be sent.";

    private final PasswordResetService passwordResetService;

    public PasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @RequestMapping(
            value = {"forgot-password", "v2/forgot-password"},
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> forgotPassword(
            @RequestParam(required = false) String email,
            @RequestBody(required = false) ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestEmail = email;
        if (!StringUtils.hasText(requestEmail) && request != null) {
            requestEmail = request.getEmail();
        }
        if (!StringUtils.hasText(requestEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email address is required"));
        }

        try {
            passwordResetService.requestReset(
                    requestEmail,
                    resolveClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent")
            );
        } catch (Exception exception) {
            // Never reveal account existence or mail infrastructure details.
            log.error("Password reset request could not be completed", exception);
        }
        return ResponseEntity.ok(Map.of("message", NEUTRAL_RESPONSE));
    }

    @RequestMapping(
            value = {"reset-password", "v2/reset-password"},
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request == null || !StringUtils.hasText(request.getToken())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Reset token is invalid or expired"));
        }
        if (!StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password is required"));
        }

        try {
            passwordResetService.resetPassword(request.getToken(), request.getPassword());
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        } catch (Exception exception) {
            log.error("Password reset failed", exception);
            return ResponseEntity.badRequest().body(Map.of("message", "Reset token is invalid or expired"));
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    public static class ForgotPasswordRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ResetPasswordRequest {
        private String token;
        private String password;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
