package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.user.UserCreateDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.dto.user.UserRoleDto;
import za.co.mawa.bes.service.UserService;

import java.util.List;

@RestController
@CrossOrigin
public class UserController {
    Gson gson = new Gson();
    @Autowired
    UserService userService;

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public ResponseEntity<?> createUser(@RequestBody UserCreateDto userCreateDto) {
        try {
            UserDto userDto = userService.create(userCreateDto);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public ResponseEntity<?> getUsers() {
        try {
            List<UserDto> userDtoList = userService.getAll();
            return ResponseEntity.ok(gson.toJson(userDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getUser(@PathVariable String id) {
        try {
            UserDto userDto = userService.getUserByName(id);
            return ResponseEntity.ok(gson.toJson(userDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/user/{id}/role", method = RequestMethod.GET)
    public ResponseEntity<?> getRoles(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(userService.getRoles(id));
    }

    @RequestMapping(value = "/user/{id}/role", method = RequestMethod.POST)
    public ResponseEntity<?> addRole(@PathVariable String id, @RequestBody List<String> roleList) throws Exception {
        try {
            for (String role : roleList) {
                UserRoleDto userRoleDto = new UserRoleDto();
                userRoleDto.setUser(id);
                userRoleDto.setRole(role);
                userService.addRole(userRoleDto);
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
