package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.RoleCreateDto;
import za.co.mawa.bes.dto.RoleDto;
import za.co.mawa.bes.dto.RoleWorkcenterDto;
import za.co.mawa.bes.entity.RoleWorkcenterPKEntity;
import za.co.mawa.bes.service.RoleService;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;

import java.util.Date;
import java.util.List;


@RestController
@CrossOrigin
public class RoleController {
    Gson gson = new Gson();
    @Autowired
    RoleService roleService;
    @RequestMapping(value = "/role", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createRole(@RequestBody RoleCreateDto roleCreateDto) {
        try {
            RoleDto roleDto = new RoleDto();
            roleDto.setId(roleCreateDto.getId());
            roleDto.setDescription(roleCreateDto.getDescription());
            roleDto.setValidFrom(new Date());
            roleDto.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            roleService.create(roleDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/role", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRoles() {
        try {
            return ResponseEntity.ok(gson.toJson(roleService.getAll()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/role/{role}/workcenter", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getWorkcenters(@PathVariable String role) {
        try {
            return ResponseEntity.ok(gson.toJson(roleService.getRoleWorkcenters(role)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/role/{role}/workcenter", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postWorkcenters(@PathVariable String role, @RequestBody List<String> workcenterList) {
        try {
            for(String workcenter: workcenterList){
                RoleWorkcenterDto roleWorkcenterDto = new RoleWorkcenterDto();
                roleWorkcenterDto.setRole(role);
                roleWorkcenterDto.setWorkcenter(workcenter);
                roleService.addWorkcenter(roleWorkcenterDto);
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/role/{role}/workcenter", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteWorkcenters(@PathVariable String role,@RequestParam(required = true) String workcenter) {

        try {
            RoleWorkcenterPKEntity pkEntity = new RoleWorkcenterPKEntity();
            pkEntity.setWorkcenter(workcenter);
            pkEntity.setRole(role);
            boolean deleted = roleService.deleteWorkcenter(pkEntity);
            return ResponseEntity.ok().body(deleted);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/role/{role}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteRole(@PathVariable String role) {
        try {
            boolean deleted = roleService.deleteRole(role);
            return ResponseEntity.ok().body(deleted);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
