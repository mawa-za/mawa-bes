package za.co.mawa.bes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.RoleDto;


@RestController
@CrossOrigin
public class RoleController {
    @RequestMapping(value = "/role", method = RequestMethod.POST)
    public ResponseEntity<?> createRole(@RequestBody RoleDto roleDto) throws Exception {
        return null;

    }

    @RequestMapping(value = "/role/{id}/workcenter", method = RequestMethod.GET)
    public ResponseEntity<?> getWorkcenters(@PathVariable String id) throws Exception {
        return null;

    }
}
