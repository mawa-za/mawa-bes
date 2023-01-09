package za.co.raretag.mawabes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.raretag.mawabes.entity.UserEntity;
import za.co.raretag.mawabes.service.UserService;

@RestController
@CrossOrigin
public class UserController {
    @Autowired
    UserService userService;

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public  ResponseEntity<?> createUser(@RequestBody UserEntity userEntity){
       return ResponseEntity.ok(userService.create(userEntity));
    }
    @RequestMapping(value = "/user/{id}/role", method = RequestMethod.GET)
    public ResponseEntity<?> getRoles(@PathVariable String id) throws Exception {
        return null;

    }
}
