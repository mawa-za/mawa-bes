package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.service.PartnerService;

@RestController
@CrossOrigin
public class PartnerController {
    Gson gson = new Gson();
    @Autowired
    PartnerService partnerService;

    @RequestMapping(value = "/partners/{partnerRole}/role", method = RequestMethod.GET)
    public ResponseEntity<?> getFieldOptions(@PathVariable String partnerRole) {
        return ResponseEntity.ok(gson.toJson(partnerService.getPartnerRoles(partnerRole)));
    }

    @RequestMapping(value = "/partners{partner}/assign", method = RequestMethod.POST)
    public ResponseEntity<?> assignPartnerRoleToPartner(@PathVariable String partner, @PathParam("role") String role) {
        try {
        boolean partnerDto = partnerService.addRole(partner, role);
        return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
