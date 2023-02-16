package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.service.PartnerService;

import java.util.ArrayList;

@RestController
@CrossOrigin
public class PartnerController {
    Gson gson = new Gson();
    @Autowired
    PartnerService partnerService;
//getPartners(String partnerRole)
    @RequestMapping(value = "/partners/{partnerRole}/role", method = RequestMethod.GET)
    public ResponseEntity<?> getPartnerRole(@PathVariable String partnerRole) {
        try{
            ArrayList<PartnerDto> partnerDto = partnerService.getPartners(partnerRole);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/partners/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getPartnerById(@PathVariable String id) {
        try{
            PartnerDto partnerDto = partnerService.get(id);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/partners/{id}/assign", method = RequestMethod.POST)
    public ResponseEntity<?> assignPartnerRoleToPartner(@PathVariable String id, @Param("role") String role) {
        try {
        boolean partnerDto = partnerService.addRole(id, role);
        return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
