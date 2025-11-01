package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.service.UserService;


@RestController
@CrossOrigin
@RequestMapping(value = "me")
public class MeController {
    Gson gson = new Gson();
    @Autowired
    UserService userService;
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCurrentUser() {
        try {
            UserDto userDto = userService.getUserByName(userService.getCurrentUser());
            userDto.setPassword(null);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
