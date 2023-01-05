package za.co.raretag.mawabes.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin
public class RoleController {
    @RequestMapping(value = "/role/{id}/workcenter", method = RequestMethod.GET)
    public ResponseEntity<?> getWorkcenters(@PathVariable String id) throws Exception {
        return null;

    }
}
