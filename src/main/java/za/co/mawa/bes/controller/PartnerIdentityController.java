package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.IdentityDto;
import za.co.mawa.bes.service.PartnerService;

@RestController
@CrossOrigin
public class PartnerIdentityController {
    Gson gson = new Gson();
    @Autowired
    PartnerService partnerService;

    @RequestMapping(value = "/identity/{id}", method = RequestMethod.POST)
    public ResponseEntity<?> addIdentity (@PathVariable String id, @RequestBody  IdentityDto identity) throws Exception{
        try{
            identity.setPartner(id);
            Boolean Identity = partnerService.addIdentity(identity);
                return ResponseEntity.ok(gson.toJson(Identity));
        }  catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
