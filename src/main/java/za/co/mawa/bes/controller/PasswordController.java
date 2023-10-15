package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.jwt.JwtTokenUtil;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.membership.MembershipCreateDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.service.EmailService;
import za.co.mawa.bes.service.EncryptionService;
import za.co.mawa.bes.service.JwtUserDetailsService;
import za.co.mawa.bes.service.UserService;

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

    @RequestMapping(value = "forgot-password", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPassword(@RequestParam String user) {
        try {
            UserDto userDto = userService.getUserByName(user);
            final String token = jwtTokenUtil.generateToken(userDto.getUsername());
            EmailDto emailDto = new EmailDto();
            emailDto.setTo(userDto.getEmail());
            emailDto.setSubject("Forgot Password");
            emailDto.setTemplate("forgot-password");
            emailService.send(emailDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
