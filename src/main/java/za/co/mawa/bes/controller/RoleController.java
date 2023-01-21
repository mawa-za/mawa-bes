package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.RoleDto;
import za.co.mawa.bes.service.RoleService;


@RestController
@CrossOrigin
public class RoleController {
    Gson gson = new Gson();
    @Autowired
    RoleService roleService;
    @RequestMapping(value = "/role", method = RequestMethod.POST)
    public ResponseEntity<?> createRole(@RequestBody RoleDto roleDto) {
        return null;

    }

    @RequestMapping(value = "/role/{id}/workcenter", method = RequestMethod.GET)
    public ResponseEntity<?> getWorkcenters(@PathVariable String id) {
        return ResponseEntity.ok(gson.toJson(roleService.getRoleWorkcenters(id)));
    }
}
