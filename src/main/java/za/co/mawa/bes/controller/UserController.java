package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.UserCreateDto;
import za.co.mawa.bes.service.UserService;

@RestController
@CrossOrigin
public class UserController {
    @Autowired
    UserService userService;
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public  ResponseEntity<?> createUser(@RequestBody UserCreateDto userCreateDto){
       return ResponseEntity.ok(userService.create(userCreateDto));
    }
    @RequestMapping(value = "/user/{id}/role", method = RequestMethod.GET)
    public ResponseEntity<?> getRoles(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(userService.getRoles(id));
    }

    @RequestMapping(value = "/user/{id}/role", method = RequestMethod.POST)
    public ResponseEntity<?> addRole(@PathVariable String id) throws Exception {
        return null;

    }
}
