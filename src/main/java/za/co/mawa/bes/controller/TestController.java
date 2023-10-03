package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.dto.user.UserCreateDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.service.EmailService;
import za.co.mawa.bes.service.UserService;
import za.co.mawa.bes.utils.HtmlTemplateVariableKey;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
public class TestController {
    Gson gson = new Gson();
    @Autowired
    EmailService emailService;

    @RequestMapping(value = "/email", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> email() {
        try {
            EmailDto emailDto = new EmailDto();
            emailDto.setTo("tebogo.mohale@mawa.co.za");
            emailDto.setSubject("Password Reset");
            emailDto.setTemplate("password-reset");
            List<PropertyDto> props = new ArrayList<>();
            props.add(new PropertyDto(HtmlTemplateVariableKey.USER_NAME,"Tebogo Mohale"));
            props.add(new PropertyDto(HtmlTemplateVariableKey.USER_PASSWORD,"Password"));
            emailDto.setProperties(props);
            emailService.send(emailDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
