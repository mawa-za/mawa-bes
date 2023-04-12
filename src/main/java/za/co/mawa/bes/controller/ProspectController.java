package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.prospect.ProspectCreateDto;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.PartnerType;


import java.util.ArrayList;
@RestController
@CrossOrigin
public class ProspectController {
    Gson gson = new Gson();

    @Autowired
    PartnerService partnerService;
    @RequestMapping(value= "/prospect" , method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createReceipt(@RequestBody ProspectCreateDto prospectCreateDto) {
        try {
            PartnerDto createDto = new PartnerDto();
            createDto.setType(prospectCreateDto.getPartnerType());
            if(prospectCreateDto.getPartnerType().equalsIgnoreCase(PartnerType.ORGANISATION))
            {
                createDto.setName1(prospectCreateDto.getOrganisationName());
            }
            else {
                if(prospectCreateDto.getFirstName() != null && prospectCreateDto.getFirstName() != "")
                {
                    createDto.setName2(prospectCreateDto.getFirstName());
                }
                if(prospectCreateDto.getMiddleName() != null && prospectCreateDto.getMiddleName() != "")
                {
                    createDto.setName3(prospectCreateDto.getMiddleName());
                }
                if(prospectCreateDto.getSurname() != null && prospectCreateDto.getSurname() != "")
                {
                    createDto.setName1(prospectCreateDto.getSurname());
                }
            }
            PartnerDto partner = partnerService.create(createDto);
            return ResponseEntity.ok(gson.toJson(partner));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }
}
