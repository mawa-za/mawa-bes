package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.RoleType;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "partner")
public class PartnerController {
    Gson gson = new Gson();
    @Autowired
    PartnerService partnerService;


    @RequestMapping( method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartner(@RequestParam(required = false) String role) throws Exception {
        try{
            PartnerQueryDto pq = new PartnerQueryDto();
                if(role != null && role != ""){
                    pq.setRole(role);
                }
        ArrayList<PartnerDto> objects = partnerService.search(pq);
        ArrayList<PersonDto> persons = new ArrayList<>();
        for (PartnerDto object : objects) {
            ArrayList<ContactDto> contactDtos = partnerService.getContacts(object.getId());
            PersonDto person = new PersonDto(object);
            persons.add(person);
        }
        String response = gson.toJson(persons);
        return ResponseEntity.ok(response);
        }catch (Exception exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/customer", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCustomer() throws Exception {
        try{
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
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postCustomer(@RequestBody PartnerDto partnerDto) throws Exception {
        try {
            String personDto = partnerService.create(partnerDto).getId();
            return ResponseEntity.ok(gson.toJson(personDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    public String putCustomer() throws Exception {
        return null;
    }
    @RequestMapping(value = "{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerById(@PathVariable String id) {
        try {
            PartnerDto partnerDto = partnerService.get(id);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignPartnerRoleToPartner(@PathVariable String id, @RequestBody List<String> roleList) {
        try {
            for(String role:roleList){
                RolePartnerDto partnerRole = new RolePartnerDto();
                partnerRole.setPartner(id);
                partnerRole.setRole(role);
                partnerService.addPartnersRole(partnerRole);
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/role", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerRole(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerRoles(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/role", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deletePartnerRole(@PathVariable String id,@RequestParam(required = true) String role) {
        try {
            PartnerRolePKEntity entity = new PartnerRolePKEntity();
            entity.setId(id);
            entity.setRole(role);
            return ResponseEntity.ok(gson.toJson(partnerService.deleteRoles(entity)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/address", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerAddress(@PathVariable String id, @RequestBody AddressDto addressDto) {
        try {
            addressDto.setPartner(id);
            boolean partnerDto = partnerService.addAddress(addressDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/address/{addressId}", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPartnerAddress(@PathVariable String addressId, @RequestBody AddressEditDto addressEditDto) {
        try {
            boolean partnerDto = partnerService.editPartnerAddress(addressId,addressEditDto);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/address", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerAddress(@PathVariable String id,@RequestParam(required = false) String type) {
        try {
            AddressQueryDto query = new AddressQueryDto();
            query.setPartnerId(id);
            if(type != null && type != "") {
                query.setType(type);
            }
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerAddress(query)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "address", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllPartnerAddress(@RequestParam(required = false) String type) {
        try {
            AddressQueryDto query = new AddressQueryDto();
            if(type != null && type != "") {
                query.setType(type);
            }
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerAddress(query)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/address", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteAddress(@PathVariable String id, @RequestParam(required = true) String addressId,
                                           @RequestParam(required = true) String type
    ) {
        try {
            PartnerAddressPKEntity pkEntity = new PartnerAddressPKEntity();
            pkEntity.setPartner(id);
            pkEntity.setAddressUsage(type);
            pkEntity.setAddressId(Integer.parseInt(addressId));
            boolean partnerDto = partnerService.removeAddress(pkEntity);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerContact(@PathVariable String id, @RequestBody ContactCreateDto contact) {
        try {
            boolean partnerDto = partnerService.addPartnerContact(id,contact);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPartnerContact(@PathVariable String id, @RequestBody ContactDto contactDto) {
        try {
            contactDto.setPartner(id);
            boolean partnerDto = partnerService.editContact(contactDto);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/contact", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerContact(@PathVariable String id, @RequestParam(required = false) String value,@RequestParam(required = false) String type) {
        try {
            ContactQueryDto contactQueryDto = new ContactQueryDto();
            contactQueryDto.setPartner(id);
            if(value != null && value != ""){
                contactQueryDto.setValue(value);
            }
            if(type != null && type != ""){
                contactQueryDto.setType(type);
            }
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerContact(contactQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/contact", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnersContact(@RequestParam(required = false) String value,@RequestParam(required = false) String type) {
        try {
            ContactQueryDto contactQueryDto = new ContactQueryDto();
            if(value != null && value != ""){
                contactQueryDto.setValue(value);
            }
            if(type != null && type != ""){
                contactQueryDto.setType(type);
            }
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerContact(contactQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/contact", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteContact(@PathVariable String id, @RequestParam(required = true) String type
    ) {
        try {
            PartnerContactPKEntity pk = new PartnerContactPKEntity();
            pk.setPartner(id);
            pk.setType(type);
            boolean partnerDto = partnerService.removePartnerContact(pk);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerIdentity(@PathVariable String id, @RequestBody IdentityDto identityDto) {
        try {
            identityDto.setPartner(id);
            boolean partnerDto = partnerService.addIdentity(identityDto);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/identity", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteIdentity(@RequestParam("idType") String type,
                                            @RequestParam("idNumber") String idValue) throws Exception {
        try {
            PartnerIdentityPKEntity pkEntity = new PartnerIdentityPKEntity();
            pkEntity.setType(type);
            pkEntity.setValue(idValue);
            return ResponseEntity.ok(partnerService.removeIdentity(pkEntity));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/identity", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPartnerIdentity(@PathVariable String id,
                                                 @RequestParam(required = true) String idType,
                                                 @RequestBody IdentityEditDto editDto) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.editPartnerIdentity(editDto,idType,id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerIdentity(@PathVariable String id,
                                                @RequestParam(required = false) String idType,
                                                @RequestParam(required = false) String idNumber) {
        try {
            IdentityQueryDto query = new IdentityQueryDto();
            query.setPartner(id);
            if(idType != null && idType != ""){
                query.setType(idType);
            }
            if(idNumber != null && idNumber != ""){
                query.setValue(idNumber);
            }
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerIdentities(query)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/identity", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnersIdentity(@RequestParam(required = false) String idType,
                                                @RequestParam(required = false) String idNumber) {
        try {
            IdentityQueryDto query = new IdentityQueryDto();
            if(idType != null && idType != ""){
                query.setType(idType);
            }
            if(idNumber != null && idNumber != ""){
                query.setValue(idNumber);
            }
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerIdentities(query)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/archive", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ArchivePartner(@PathVariable String id) {
        try {

            boolean partnerDto = partnerService.archive(id);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/unarchive", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> UnarchivePartner(@PathVariable String id) {
        try {

            boolean partnerDto = partnerService.unArchive(id);

            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}",method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPartner(@PathVariable String id,@RequestBody PartnerEditDto editPartner){
        try{
            return ResponseEntity.ok(gson.toJson(gson.toJson(partnerService.editPartner(editPartner,id))));
        }catch(Exception exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/attachment",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAttachPartner(@PathVariable String id){
        try{
            return ResponseEntity.ok(gson.toJson(partnerService.getAttachments(id)));
        }catch(Exception exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/attachment",method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeAttachPartner(@PathVariable String id,@RequestParam String fileType){
        try{
            PartnerAttachmentPKEntity pkEntity = new PartnerAttachmentPKEntity();
            pkEntity.setFileType(fileType);
            pkEntity.setPartner(id);
            return ResponseEntity.ok(gson.toJson(partnerService.removeAttachment(pkEntity)));
        }catch(Exception exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
