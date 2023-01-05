package za.co.raretag.mawabes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class UserController {
    @RequestMapping(value = "/user/{id}/role", method = RequestMethod.GET)
    public ResponseEntity<?> getRoles(@PathVariable String id) throws Exception {
        return null;

    }
}
