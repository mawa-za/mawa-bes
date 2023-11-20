package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.IdentityDto;
import za.co.mawa.bes.dto.partner.PartnerIdentityCreateDto;
import za.co.mawa.bes.service.PartnerIdentityService;
import za.co.mawa.bes.service.PartnerService;

@RestController
@CrossOrigin
public class PartnerIdentityController {
    Gson gson = new Gson();
    @Autowired
    PartnerService partnerService;

    @Autowired
    PartnerIdentityService partnerIdentityService;

    @RequestMapping(value = "/identity/{id}", method = RequestMethod.POST)
    public ResponseEntity<?> addIdentity(@PathVariable String id, @RequestBody PartnerIdentityCreateDto partnerIdentityCreateDto) throws Exception {
        try {
            partnerIdentityCreateDto.setPartner(id);
            partnerIdentityService.add(partnerIdentityCreateDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
