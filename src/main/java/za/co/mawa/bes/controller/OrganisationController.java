package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.OrganizationDto;
import za.co.mawa.bes.repository.*;
import za.co.mawa.bes.service.OrganizationService;

@RestController
@CrossOrigin
public class OrganisationController {
    Gson gson = new Gson();

    @Autowired
    OrganizationService organizationService;
    @Autowired
    PartnerRoleRepository partnerRoleRepository;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    @Autowired
    PartnerContactRepository partnerContactRepository;
    @Autowired
    PartnerAddressRepository partnerAddressRepository;
    @Autowired
    PartnerAttachmentRepository partnerAttachmentRepository;
    @RequestMapping(value = "/Organization", method = RequestMethod.POST)
    public ResponseEntity<?> postOrganization (@RequestBody OrganizationDto organizationDto){
        try{
            return ResponseEntity.ok(gson.toJson(organizationService.addOrganization(organizationDto)));
        }  catch (Exception exception) {
                  return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
              }
    }

    @RequestMapping(value = "/Organization", method = RequestMethod.GET)
         public ResponseEntity<?> getOrganizations() {
              try {
                  return ResponseEntity.ok(gson.toJson(organizationService.getOrganizations()));
             } catch (Exception exception) {
                  return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
              }
    }

    @RequestMapping(value = "/Organization{id}/Roles", method = RequestMethod.GET)
    public ResponseEntity<?> getRolesResource(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerRoleRepository.findPartnerByRole(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/Organization{id}/Identities", method = RequestMethod.GET)
    public ResponseEntity<?> getIdentitiesResource(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerIdentityRepository.findPartnerIdentityByPartner(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/Organization{id}/Contacts", method = RequestMethod.GET)
    public ResponseEntity<?> getContactsResource(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerContactRepository.findPartnerByValue(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/Organization{id}/Addresses", method = RequestMethod.GET)
    public ResponseEntity<?> getAddressesResource(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerAddressRepository.findPartnerAddressByPartner(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/Organization{id}/Attachments", method = RequestMethod.GET)
    public ResponseEntity<?> getAttachmentsResource(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerAttachmentRepository.findByPartner(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
