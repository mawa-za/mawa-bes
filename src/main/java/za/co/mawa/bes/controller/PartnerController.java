package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.partner.*;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.service.PartnerBankAccountService;
import za.co.mawa.bes.service.AddressService;
import za.co.mawa.bes.service.PartnerAddressService;
import za.co.mawa.bes.service.PartnerIdentityService;
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
    @Autowired
    PartnerIdentityService partnerIdentityService;
    @Autowired
    PartnerBankAccountService partnerBankAccountService;
    @Autowired
    PartnerAddressService partnerAddressService;
    @Autowired
    AddressService addressService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartner(@RequestParam(required = false) String role,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String attributeName,
                                        @RequestParam(required = false) String attributeValue) throws Exception {
        try {
            PartnerQueryDto partnerQueryDto = new PartnerQueryDto();
            if (role != null && role != "") {
                partnerQueryDto.setRole(role);
            }
            if (type != null && type != "") {
                partnerQueryDto.setType(type);
            }
            if (attributeName != null && attributeName != "") {
                partnerQueryDto.setAttributeName(attributeName);
                partnerQueryDto.setAttributeValue(attributeValue);
            }
            String response = gson.toJson(partnerService.search(partnerQueryDto));
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postCustomer(@RequestBody PartnerCreateDto partnerCreateDto) throws Exception {
        try {
            String personDto = partnerService.create(partnerCreateDto).getId();
            return ResponseEntity.ok(gson.toJson(personDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPartner(@PathVariable String id, @RequestBody PartnerEditDto partnerEditDto) {
        try {
            partnerService.edit(partnerEditDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerById(@PathVariable String id) {
        try {
            PartnerDto partnerDto = partnerService.get(id);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignPartnerRoleToPartner(@PathVariable String id, @RequestBody List<String> roleList) {
        try {
            for (String role : roleList) {
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

    @RequestMapping(value = "{id}/role", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerRole(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerRoles(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deletePartnerRole(@PathVariable String id, @RequestParam(required = true) String role) {
        try {
            PartnerRolePKEntity entity = new PartnerRolePKEntity();
            entity.setId(id);
            entity.setRole(role);
            return ResponseEntity.ok(gson.toJson(partnerService.deleteRoles(entity)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerContact(@PathVariable String id, @RequestBody ContactCreateDto contact) {
        try {
            boolean partnerDto = partnerService.addPartnerContact(id, contact);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPartnerContact(@PathVariable String id,
                                                @RequestBody ContactEditDto contactDto,
                                                @RequestParam String contactType) {
        try {
            PartnerContactPKEntity entityPk = new PartnerContactPKEntity();
            entityPk.setType(contactType);
            entityPk.setPartner(id);
            boolean partnerDto = partnerService.contactEdit(entityPk, contactDto);
            return ResponseEntity.ok(gson.toJson(partnerDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerContact(@PathVariable String id, @RequestParam(required = false) String value, @RequestParam(required = false) String type) {
        try {
            ContactQueryDto contactQueryDto = new ContactQueryDto();
            contactQueryDto.setPartner(id);
            if (value != null && value != "") {
                contactQueryDto.setValue(value);
            }
            if (type != null && type != "") {
                contactQueryDto.setType(type);
            }
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerContact(contactQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/contact", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnersContact(@RequestParam(required = false) String value, @RequestParam(required = false) String type) {
        try {
            ContactQueryDto contactQueryDto = new ContactQueryDto();
            if (value != null && value != "") {
                contactQueryDto.setValue(value);
            }
            if (type != null && type != "") {
                contactQueryDto.setType(type);
            }
            return ResponseEntity.ok(gson.toJson(partnerService.getPartnerContact(contactQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "{id}/identity", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerIdentity(@PathVariable String id, @RequestBody PartnerIdentityCreateDto partnerIdentityCreateDto) {
        try {
            partnerIdentityCreateDto.setPartner(id);
            partnerIdentityService.add(partnerIdentityCreateDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            String errorMessage = exception.getMessage();
            int index = errorMessage.indexOf(":");
            String exceptionMessage = (index != -1) ? errorMessage.substring(index + 1).trim() : errorMessage;
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionMessage);
        }
    }

    @RequestMapping(value = "/identity", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getIdentity(@RequestParam("idType" ) String type,
                                         @RequestParam("idNumber") String idValue) throws Exception {
        try {
           return ResponseEntity.ok(partnerIdentityService.getIdentity(type,idValue));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/identity", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "{id}/identity", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPartnerIdentity(@PathVariable String id,
                                                 @RequestBody PartnerIdentityEditDto partnerIdentityEditDto) {
        try {
            partnerIdentityService.edit(partnerIdentityEditDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPartnerIdentity(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerIdentityService.getAll(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/archive", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ArchivePartner(@PathVariable String id) {
        try {
            partnerService.archive(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/unarchive", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> UnarchivePartner(@PathVariable String id) {
        try {
            partnerService.unArchive(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerAttribute(@PathVariable String id, @RequestBody PartnerAttributeCreateDto createDto) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.addAttribute(createDto)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeAttribute(@PathVariable String id, @RequestParam String attribute) {
        try {
            PartnerAttributePKEntity pkEntity = new PartnerAttributePKEntity();
            pkEntity.setAttribute(attribute);
            pkEntity.setPartner(id);
            return ResponseEntity.ok(gson.toJson(partnerService.deleteAttribute(pkEntity)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editAttribute(@PathVariable String id, @RequestBody PartnerAttributeEditDto editDto, @RequestParam String attribute) {
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.editAttribute(editDto, id, attribute)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAttribute(@PathVariable String id) {
        try {
            PartnerAttributeQueryDto queryDto = new PartnerAttributeQueryDto();
            queryDto.setPartner(id);
            return ResponseEntity.ok(gson.toJson(partnerService.getAttributes(queryDto)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "relation", method = RequestMethod.POST , produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRelation(@RequestBody RelationDto relation){
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.addRelation(relation)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    };

    @RequestMapping(value = "relation/{id}", method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllRelation(@PathVariable String id){
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.getAllRelations(id)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    };

    @RequestMapping(value = "relation", method = RequestMethod.GET , produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRelation(@RequestBody RelationDto relation){
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.getRelationByBothIds(relation)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    };

    @RequestMapping(value = "relation", method = RequestMethod.DELETE , produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteRelation(@RequestBody RelationDto relation){
        try {
            return ResponseEntity.ok(gson.toJson(partnerService.removeRelation(relation)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    };

}
