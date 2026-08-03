package za.co.mawa.bes.controller.v2;

import com.nimbusds.jose.shaded.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.partner.*;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.supplier.SupplierOnboardingRequest;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.exception.DuplicateCreationException;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.service.v2.SupplierApprovalService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping(value = "v2/partner")
public class PartnerControllerV2 {
    Gson gson = new Gson();
    @Autowired
    PartnerService partnerService;
    @Autowired
    PartnerServiceV2 partnerServiceV2;
    @Autowired
    PartnerIdentityService partnerIdentityService;
    @Autowired
    PartnerIdentityServiceV2 partnerIdentityServiceV2;
    @Autowired
    PartnerBankAccountService partnerBankAccountService;
    @Autowired
    PartnerAddressService partnerAddressService;
    @Autowired
    AddressService addressService;
    @Autowired
    SupplierApprovalService supplierApprovalService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PartnerViewEntity>> getPartners(@RequestParam(required = false) String query, @RequestParam(required = false) String role) {
        try {
            query = query == null ? "" : query.trim();
            role = role == null ? "" : role.trim();
            List<PartnerViewEntity> partnerViewEntities = new ArrayList<>();
            if (!query.isEmpty() && !role.isEmpty()) {
                partnerViewEntities = partnerServiceV2.searchByStringAndRole('%' + query + '%', role);
            } else if (!query.isEmpty()) {
                partnerViewEntities = partnerServiceV2.searchByString('%' + query + '%');
            } else if (!role.isEmpty()) {
                partnerViewEntities = partnerServiceV2.getByRole(role);
            } else {
                partnerViewEntities = partnerServiceV2.getAll();
            }
            List<PartnerViewEntity> uniquePartners = new ArrayList<>(
                    partnerViewEntities.stream()
                            .collect(Collectors.toMap(
                                    PartnerViewEntity::getPartnerId,
                                    p -> p,
                                    (existing, replacement) -> existing
                            ))
                            .values()
            );
            return ResponseEntity.ok(uniquePartners);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> post(@RequestBody PartnerInboundDto partnerInboundDto) {
        try {
            if ("SUPPLIER".equalsIgnoreCase(partnerInboundDto.getPartnerRole())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            boolean existingPartner = partnerServiceV2.identityExists(
                    partnerInboundDto.getIdentityType(), partnerInboundDto.getIdentityNumber());
            PartnerViewEntity partnerViewEntity = partnerServiceV2.create(partnerInboundDto);
            PartnerOutboundDto partnerOutboundDto = new PartnerOutboundDto();
            partnerOutboundDto.setPartnerId(partnerViewEntity.getPartnerId());
            partnerOutboundDto.setExistingPartner(existingPartner);
            partnerOutboundDto.setPartnerNo(partnerViewEntity.getPartnerNo());
            partnerOutboundDto.setIdentityType(partnerViewEntity.getIdentityType());
            partnerOutboundDto.setIdentityNumber(partnerViewEntity.getIdentityNumber());
            partnerOutboundDto.setName1(partnerViewEntity.getName1());
            partnerOutboundDto.setName2(partnerViewEntity.getName2());
            partnerOutboundDto.setName3(partnerViewEntity.getName3());
            return ResponseEntity.status(existingPartner ? HttpStatus.OK : HttpStatus.CREATED)
                    .body(partnerOutboundDto);
        } catch (Exception exception) {
            Throwable root = exception;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            log.error("Unable to create partner: {}", root.getMessage(), exception);
            String message = root instanceof IllegalArgumentException
                    ? root.getMessage()
                    : "Unable to create partner. Review the supplied information and try again";
            if (message == null || message.isBlank()) {
                message = "Unable to create partner. Review the supplied information and try again";
            }
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(message, HttpStatus.BAD_REQUEST.value())
            );
        }
    }

    @PostMapping(value = "/supplier/submit-for-approval", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApprovalRequestResponse> submitSupplierForApproval(
            @RequestBody SupplierOnboardingRequest onboardingRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                supplierApprovalService.submitSupplierOnboarding(
                        onboardingRequest, resolveActor(userId, principal))
        );
    }

    @GetMapping(value = "/{id}/bank-accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PartnerBankAccountGetDto> getSupplierBankAccounts(@PathVariable String id) {
        return ResponseEntity.ok(partnerBankAccountService.getBankAccounts(id));
    }

    @PostMapping(value = "/{id}/bank-accounts/submit-for-approval", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApprovalRequestResponse> submitSupplierBankingForApproval(
            @PathVariable String id,
            @RequestBody PartnerBankAccountDto bankAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            Principal principal
    ) {
        return ResponseEntity.ok(
                supplierApprovalService.submitBankingDetails(
                        id, bankAccount, resolveActor(userId, principal))
        );
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PartnerEditDto> editPartner(@PathVariable String id, @RequestBody PartnerEditDto partnerEditDto) {
        try {
            partnerService.edit(partnerEditDto);
            return ResponseEntity.ok(partnerEditDto);
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PartnerDto> getPartnerById(@PathVariable String id)
            throws PartnerNotFoundException {
        return ResponseEntity.ok(partnerService.get(id));
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignPartnerRoleToPartner(@PathVariable String id, @RequestBody List<String> roleList) {
        try {
            if (roleList != null && roleList.stream().anyMatch(role -> "SUPPLIER".equalsIgnoreCase(role))) {
                throw new IllegalArgumentException(
                        "Supplier role must be assigned through the supplier onboarding approval process");
            }
            for (String role : roleList) {
                RolePartnerDto partnerRole = new RolePartnerDto();
                partnerRole.setPartner(id);
                partnerRole.setRole(role);
                partnerService.addPartnersRole(partnerRole);
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RoleDto>> getPartnerRole(@PathVariable String id) {
        try {
            return ResponseEntity.ok(partnerService.getPartnerRoles(id));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/role", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deletePartnerRole(@PathVariable String id, @RequestParam(required = true) String role) {
        try {
            PartnerRolePKEntity entity = new PartnerRolePKEntity();
            entity.setId(id);
            entity.setRole(role);
            partnerService.deleteRoles(entity);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerContact(@PathVariable String id, @RequestBody ContactCreateDto contact) {
        try {
            partnerService.addPartnerContact(id, contact);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
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
            partnerService.contactEdit(entityPk, contactDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ContactGetDto>> getPartnerContact(@PathVariable String id, @RequestParam(required = false) String value, @RequestParam(required = false) String type) {
        try {
            ContactQueryDto contactQueryDto = new ContactQueryDto();
            contactQueryDto.setPartner(id);
            if (value != null && value != "") {
                contactQueryDto.setValue(value);
            }
            if (type != null && type != "") {
                contactQueryDto.setType(type);
            }
            return ResponseEntity.ok(partnerService.getPartnerContact(contactQueryDto));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "/contact", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ContactGetDto>> getPartnersContact(@RequestParam(required = false) String value, @RequestParam(required = false) String type) {
        try {
            ContactQueryDto contactQueryDto = new ContactQueryDto();
            if (value != null && value != "") {
                contactQueryDto.setValue(value);
            }
            if (type != null && type != "") {
                contactQueryDto.setType(type);
            }
            return ResponseEntity.ok(partnerService.getPartnerContact(contactQueryDto));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/contact", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteContact(@PathVariable String id, @RequestParam(required = true) String type
    ) {
        try {
            PartnerContactPKEntity pk = new PartnerContactPKEntity();
            pk.setPartner(id);
            pk.setType(type);
            partnerService.removePartnerContact(pk);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerIdentity(@PathVariable String id, @RequestBody PartnerIdentityCreateDto partnerIdentityCreateDto) {
        try {
            partnerIdentityCreateDto.setPartner(id);
            partnerIdentityService.add(partnerIdentityCreateDto);
            return ResponseEntity.ok().build();
        } catch (DuplicateCreationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @RequestMapping(value = "/identity", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PartnerIdentityDto> getIdentity(@RequestParam("idType") String type,
                                         @RequestParam("idNumber") String idValue) throws Exception {
        try {
            return ResponseEntity.ok(partnerIdentityService.getIdentity(type, idValue));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "/identity", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteIdentity(@RequestParam("idType") String type,
                                            @RequestParam("idNumber") String idValue) throws Exception {
        try {
            PartnerIdentityPKEntity pkEntity = new PartnerIdentityPKEntity();
            pkEntity.setType(type);
            pkEntity.setValue(idValue);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPartnerIdentity(@PathVariable String id,
                                                 @RequestBody PartnerIdentityEditDto partnerIdentityEditDto) {
        try {
            partnerIdentityService.edit(partnerIdentityEditDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/identity", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PartnerIdentityDto>> getPartnerIdentity(@PathVariable String id) {
        try {
            return ResponseEntity.ok(partnerIdentityService.getAll(id));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/archive", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ArchivePartner(@PathVariable String id) {
        try {
            partnerService.archive(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/unarchive", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> UnarchivePartner(@PathVariable String id) {
        try {
            partnerService.unArchive(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addPartnerAttribute(@PathVariable String id, @RequestBody PartnerAttributeCreateDto createDto) {
        try {
            partnerService.addAttribute(createDto);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeAttribute(@PathVariable String id, @RequestParam String attribute) {
        try {
            PartnerAttributePKEntity pkEntity = new PartnerAttributePKEntity();
            pkEntity.setAttribute(attribute);
            pkEntity.setPartner(id);
            partnerService.deleteAttribute(pkEntity);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editAttribute(@PathVariable String id, @RequestBody PartnerAttributeEditDto editDto, @RequestParam String attribute) {
        try {
            partnerService.editAttribute(editDto, id, attribute);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "{id}/attribute", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PartnerAttribute>> getAttribute(@PathVariable String id) {
        try {
            PartnerAttributeQueryDto queryDto = new PartnerAttributeQueryDto();
            queryDto.setPartner(id);
            return ResponseEntity.ok(partnerService.getAttributes(queryDto));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }


    private String resolveActor(String headerUserId, Principal principal) {
        if (headerUserId != null && !headerUserId.isBlank()) return headerUserId.trim();
        if (UserContext.getCurrentUserId() != null && !UserContext.getCurrentUserId().isBlank()) {
            return UserContext.getCurrentUserId();
        }
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        if (UserContext.getCurrentUser() != null && !UserContext.getCurrentUser().isBlank()) {
            return UserContext.getCurrentUser();
        }
        return "SYSTEM";
    }

}