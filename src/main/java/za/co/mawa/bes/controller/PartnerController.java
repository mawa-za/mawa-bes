package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PartnerQueryDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.RoleType;

import java.util.ArrayList;

@RestController
@CrossOrigin
@RequestMapping(value = "partner")
public class PartnerController {
    Gson gson = new Gson();
    @Autowired
    PartnerService partnerService;
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getCustomer() throws Exception{
        PartnerQueryDto query = new PartnerQueryDto();
        query.setRole(RoleType.CUSTOMER);
        ArrayList<PartnerDto> objects = partnerService.search(query);
        ArrayList<PersonDto> persons = new ArrayList<>();
        for(PartnerDto object : objects)
        {
            PersonDto person = new PersonDto(object);
            persons.add(person);
        }
        String response = gson.toJson(persons);
        return ResponseEntity.ok(response);
    }
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> postCustomer(@RequestBody PartnerDto partnerDto) throws Exception{
        try{
            String personDto = partnerService.create(partnerDto).getId();
            return ResponseEntity.ok(gson.toJson(personDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(method = RequestMethod.PUT)
    public String putCustomer() throws Exception{
        return null;
    }
    @RequestMapping(value = "{partnerRole}/role", method = RequestMethod.GET)
    public ResponseEntity<?> getPartnerRole(@PathVariable String partnerRole) {
        try{
            ArrayList<PartnerDto> partnerDto = partnerService.getPartners(partnerRole);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getPartnerById(@PathVariable String id) {
        try{
            PartnerDto partnerDto = partnerService.get(id);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/assign", method = RequestMethod.POST)
    public ResponseEntity<?> assignPartnerRoleToPartner(@PathVariable String id, @Param("role") String role) {
        try {
        boolean partnerDto = partnerService.addRole(id, role);
        return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
