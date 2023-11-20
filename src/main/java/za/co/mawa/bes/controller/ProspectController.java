package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.partner.PartnerCreateDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.prospect.ProspectCreateDto;
import za.co.mawa.bes.dto.prospect.ProspectDto;
import za.co.mawa.bes.dto.prospect.ProspectEditDto;
import za.co.mawa.bes.dto.prospect.ProspectSearchDto;
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
            PartnerCreateDto partnerCreateDto = new PartnerCreateDto();
//            createDto.setType(prospectCreateDto.getPartnerType());
            if(prospectCreateDto.getPartnerType().equalsIgnoreCase(PartnerType.ORGANIZATION))
            {
                partnerCreateDto.setName1(prospectCreateDto.getOrganisationName());
            }
            else {
                if(prospectCreateDto.getFirstName() != null && prospectCreateDto.getFirstName() != "")
                {
                    partnerCreateDto.setName2(prospectCreateDto.getFirstName());
                }
                if(prospectCreateDto.getMiddleName() != null && prospectCreateDto.getMiddleName() != "")
                {
                    partnerCreateDto.setName3(prospectCreateDto.getMiddleName());
                }
                if(prospectCreateDto.getSurname() != null && prospectCreateDto.getSurname() != "")
                {
                    partnerCreateDto.setName1(prospectCreateDto.getSurname());
                }
            }
            PartnerDto partner = partnerService.create(partnerCreateDto);
            return ResponseEntity.ok(gson.toJson(partner));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value= "/prospect/{id}" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProspect(@PathVariable String id) {
        try {
            ProspectDto partner = partnerService.getProspect(id);
            return ResponseEntity.ok(gson.toJson(partner));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }

    @RequestMapping(value= "/prospects" , method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProspects(@RequestParam(required = false) String partnerType,
                                          @RequestParam(required = false) String partnerNumber,
                                          @RequestParam(required = false) String surname,
                                          @RequestParam(required = false) String firstName,
                                          @RequestParam(required = false) String middleName,
                                          @RequestParam(required = false) String organisationName) {
        try {

            ProspectSearchDto search = new ProspectSearchDto();
            if(partnerType != null && partnerType != "")
            {
                search.setPartnerType(partnerType.toUpperCase());
            }
            if(partnerNumber != null && partnerNumber != "")
            {
                search.setPartnerNumber(partnerNumber);
            }
            if(surname != null && surname != "")
            {
                search.setSurname(surname.toUpperCase());
            }
            if(firstName != null && firstName != "")
            {
                search.setFirstName(firstName.toUpperCase());
            }
            if(middleName != null && middleName != "")
            {
                search.setMiddleName(middleName.toUpperCase());
            }
            if(organisationName != null && organisationName != "")
            {
                search.setOrganisationName(organisationName.toUpperCase());
            }
            ArrayList<ProspectDto> prospects = partnerService.getProspects(search);
            return ResponseEntity.ok(gson.toJson(prospects));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }

    @RequestMapping(value= "/prospect/{id}" , method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editProspect(@PathVariable String id, @RequestBody ProspectEditDto prospectEditDto) {
        try {
            boolean edited = partnerService.editProspect(id,prospectEditDto);
            return ResponseEntity.ok(gson.toJson(edited));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }
}
