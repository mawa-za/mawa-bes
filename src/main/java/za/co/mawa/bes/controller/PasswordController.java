package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.jwt.JwtTokenUtil;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.service.EmailService;
import za.co.mawa.bes.service.EncryptionService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.UserService;
import za.co.mawa.bes.utils.PasswordStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class PasswordController {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    EncryptionService encryptionService;
    @Autowired
    EmailService emailService;
    @Autowired
    UserService userService;
    @Autowired
    SettingService settingService;
    @Autowired
    UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secret;

    @RequestMapping(
            value = {"forgot-password", "v2/forgot-password"},
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> forgotPassword(
            @RequestParam(required = false) String email,
            @RequestBody(required = false) ForgotPasswordRequest request
    ) {
        try {
            String requestEmail = email;
            if ((requestEmail == null || requestEmail.isBlank()) && request != null) {
                requestEmail = request.getEmail();
            }
            if (requestEmail == null || requestEmail.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email address is required"));
            }

            UserEntity userEntity = userRepository.getByEmail(requestEmail.trim());
            if (userEntity == null) {
                // Do not reveal whether the email exists.
                return ResponseEntity.ok().build();
            }

            String tenant = settingService.getSetting("ACCESS-URL", "TENANT");
            if (tenant == null || tenant.isBlank()) {
                tenant = TenantContext.getCurrentTenantURL();
            }

            final String token = jwtTokenUtil.generateToken(userEntity.getUsername());
            String resetLink = buildResetEmail(tenant, token);

            EmailDto emailDto = new EmailDto();
            emailDto.setTo(userEntity.getEmail());
            emailDto.setSubject("Reset Password");
            emailDto.setTemplate("reset-password");
            List<PropertyDto> properties = new ArrayList<>();
            properties.add(new PropertyDto("resetLink", resetLink));
            emailDto.setProperties(properties);
            emailService.send(emailDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", exception.getMessage()));
        }
    }

    @RequestMapping(
            value = {"reset-password", "v2/reset-password"},
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            if (request == null || request.getToken() == null || request.getToken().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Reset token is required"));
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "New password is required"));
            }

            String username = jwtTokenUtil.getUsernameFromToken(request.getToken().trim());
            UserEntity userEntity = userRepository.getByName(username);
            if (userEntity == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid reset token"));
            }

            userEntity.setPassword(encryptionService.encrypt(request.getPassword(), secret).getBytes());
            userEntity.setPasswordStatus(PasswordStatus.PRODUCTIVE);
            userRepository.save(userEntity);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Reset token is invalid or expired"));
        }
    }

    public String buildResetEmail(String domain, String token) {
        return domain.startsWith("http://") || domain.startsWith("https://")
                ? domain + "/#/reset-password?token=" + token
                : "https://" + domain + "/#/reset-password?token=" + token;
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
