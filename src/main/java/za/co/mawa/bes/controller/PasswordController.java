package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.jwt.JwtTokenUtil;
import org.springframework.web.servlet.ModelAndView;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PasswordResetDto;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.membership.MembershipCreateDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.dto.user.UserUpdateDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.HtmlTemplateVariableKey;

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
    TenantAdminService tenantAdminService;
    @Autowired
    UserRepository userRepository;

    @RequestMapping(value = "forgot-password", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPassword(@RequestParam String user) {
        try {
            UserDto userDto = userService.getUserByName(user);
            String url = null;
            final String token = jwtTokenUtil.generateToken(userDto.getUsername());
            List<TenantDto> tenants = tenantAdminService.getAll();
            for(TenantDto tenantDto : tenants){
                if(tenantDto.getId().equalsIgnoreCase(TenantContext.getCurrentTenant())){
                    url = tenantDto.getUrl() + "/#/forgot-password/reset/" + token;
                }
            }
//            String link = "https://dev.api.app.mawa.co.za/forgot-password/reset/"+ token ;
            if(url==null){
                throw new RuntimeException("Tenant not found");
            }

            EmailDto emailDto = new EmailDto();
            emailDto.setTo(userDto.getEmail());
            emailDto.setSubject("Forgot Password");
            emailDto.setTemplate("forgot-password");
            List<PropertyDto> props = new ArrayList<>();
            props.add(new PropertyDto(HtmlTemplateVariableKey.USER_NAME, user));
            props.add(new PropertyDto(HtmlTemplateVariableKey.LINK,url ));
            emailDto.setProperties(props);
            emailService.send(emailDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(value = "password/reset", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetDto passwordResetDto) {
        try {
            String username = jwtTokenUtil.getUsernameFromToken(passwordResetDto.getToken());
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
            }

            UserEntity userEntity = userRepository.getByName(username);

            if (userEntity == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }

            UserUpdateDto userUpdateDto = new UserUpdateDto();
            userUpdateDto.setId(userEntity.getId());
            userUpdateDto.setPassword(passwordResetDto.getNewPassword());
            userService.updatePassword(userUpdateDto);

            return ResponseEntity.ok("Password reset successful.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error resetting password: " + e.getMessage());
        }
    }
}
