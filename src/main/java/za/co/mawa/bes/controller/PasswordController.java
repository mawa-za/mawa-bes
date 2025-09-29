package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.jwt.JwtTokenUtil;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.dto.membership.MembershipCreateDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.service.EmailService;
import za.co.mawa.bes.service.EncryptionService;
import za.co.mawa.bes.service.JwtUserDetailsService;
import za.co.mawa.bes.service.UserService;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.List;

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
    UserRepository userRepository;

    @RequestMapping(value = "forgot-password", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            UserEntity userEntity = userRepository.getByEmail(email);
            String tenant = TenantContext.getCurrentTenantURL();
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    public String buildResetEmail(String domain, String token) {
               // Build reset URL
        return domain.startsWith("http://") || domain.startsWith("https://")
                ? domain + "/#/reset-password?token=" + token
                : "https://" + domain + "/#/reset-password?token=" + token;
    }
}
