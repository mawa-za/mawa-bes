package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
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


    @RequestMapping( method = RequestMethod.GET)
    public ResponseEntity<?> getPartner() throws Exception {


        ArrayList<PartnerDto> objects = partnerService.search(null);
        ArrayList<PersonDto> persons = new ArrayList<>();
        for (PartnerDto object : objects) {
            ArrayList<ContactDto> contactDtos = partnerService.getContacts(object.getId());
            PersonDto person = new PersonDto(object);
            persons.add(person);
        }
        String response = gson.toJson(persons);
        return ResponseEntity.ok(response);
    }
    @RequestMapping(value = "/customer", method = RequestMethod.GET)
    public ResponseEntity<?> getCustomer() throws Exception {
        PartnerQueryDto query = new PartnerQueryDto();
        query.setRole(RoleType.CUSTOMER);
        ArrayList<PartnerDto> objects = partnerService.search(query);
        ArrayList<PersonDto> persons = new ArrayList<>();
        for (PartnerDto object : objects) {
            PersonDto person = new PersonDto(object);
            persons.add(person);
        }
        String response = gson.toJson(persons);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> postCustomer(@RequestBody PartnerDto partnerDto) throws Exception {
        try {
            String personDto = partnerService.create(partnerDto).getId();
            return ResponseEntity.ok(gson.toJson(personDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    public String putCustomer() throws Exception {
        return null;
    }

    @RequestMapping(value = "{partnerRole}/role", method = RequestMethod.GET)
    public ResponseEntity<?> getPartnerRole(@PathVariable String partnerRole) {
        try {
            ArrayList<PartnerDto> partnerDto = partnerService.getPartners(partnerRole);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getPartnerById(@PathVariable String id) {
        try {
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

    @RequestMapping(value = "{id}/address", method = RequestMethod.POST)
    public ResponseEntity<?> addPartnerAddress(@PathVariable String id, @RequestBody AddressDto addressDto) {
        try {
            addressDto.setPartner(id);
            boolean partnerDto = partnerService.addAddress(addressDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/address", method = RequestMethod.PUT)
    public ResponseEntity<?> editPartnerAddress(@PathVariable String id, @RequestBody AddressDto addressDto) {
        try {
            addressDto.setPartner(id);
            boolean partnerDto = partnerService.editAddress(addressDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/address", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteAddress(@PathVariable String id, @RequestParam("addressId") String addressId,
                                           @RequestParam("type") String type
    ) {
        try {

            AddressDto addressDto = new AddressDto();
            addressDto.setPartner(id);

            if (addressId != null) {
                addressDto.setId(Integer.parseInt(addressId));
            }
            if (type != null) {
                addressDto.setType(type);
            }
            boolean partnerDto = partnerService.removeAddress(addressDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.POST)
    public ResponseEntity<?> addPartnerContact(@PathVariable String id, @RequestBody ContactDto contactDto) {
        try {
            contactDto.setPartner(id);
            boolean partnerDto = partnerService.addContact(contactDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.PUT)
    public ResponseEntity<?> editPartnerContact(@PathVariable String id, @RequestBody ContactDto contactDto) {
        try {
            contactDto.setPartner(id);
            boolean partnerDto = partnerService.editContact(contactDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteContact(@PathVariable String id, @RequestParam("type") String type
    ) {
        try {
            ContactDto contactDto = new ContactDto();
            contactDto.setPartner(id);
            if (type != null) {
                contactDto.setType(type);
            }

            boolean partnerDto = partnerService.removeContact(contactDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.POST)
    public ResponseEntity<?> addPartnerIdentity(@PathVariable String id, @RequestBody IdentityDto identityDto) {
        try {
            identityDto.setPartner(id);
            boolean partnerDto = partnerService.addIdentity(identityDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteIdentity(@PathVariable String id,
                                            @RequestParam("idType") String type,
                                            @RequestParam("idNumber") String idValue) throws Exception {
        try {
            IdentityDto identity = new IdentityDto();
            identity.setPartner(id);
            if (type != null) {
                identity.setIdType(type);
            }
            if (idValue != null) {
                identity.setIdNumber(idValue);
            }
            boolean Deleted = partnerService.removeIdentity(identity);
            return ResponseEntity.ok(Deleted);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.PUT)
    public ResponseEntity<?> editPartnerIdentity(@PathVariable String id, @RequestBody IdentityDto identityDto) {
        try {
            identityDto.setPartner(id);
            boolean partnerDto = partnerService.editIdentity(identityDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @RequestMapping(value = "{id}/archive", method = RequestMethod.PUT)
    public ResponseEntity<?> ArchivePartner(@PathVariable String id) {
        try {

            boolean partnerDto = partnerService.archive(id);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "{id}/unarchive", method = RequestMethod.PUT)
    public ResponseEntity<?> UnarchivePartner(@PathVariable String id) {
        try {

            boolean partnerDto = partnerService.unArchive(id);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
