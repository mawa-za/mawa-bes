package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.membership.MembershipCreateDto;
import za.co.mawa.bes.dto.membership.MembershipEditDto;
import za.co.mawa.bes.dto.membership.MembershipQueryDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemEditDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping(value = "membership")
public class MembershipController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    MembershipService membershipService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    DependentService dependentService;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;

    @Autowired
    FieldOptionService fieldOptionService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postMembership(@RequestBody MembershipCreateDto membershipCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(membershipService.create(membershipCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMemberships(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String partnerFunction,
                                            @RequestParam(required = false) String partnerId,
                                            @RequestParam(required = false) String productId,
                                            @RequestParam(required = false) String salesRepresentativeId,
                                            @RequestParam(required = false) String dateJoined,
                                            @RequestParam(required = false) String idNumber) {
        try {
            MembershipQueryDto membershipQueryDto = new MembershipQueryDto();
            if(status != null && status != "") {
                membershipQueryDto.setStatus(status);
            }

            if(productId != null && productId != "") {
                membershipQueryDto.setProductId(productId);
            }

            if(salesRepresentativeId != null && salesRepresentativeId != "") {
                membershipQueryDto.setSalesRepresentativeId(salesRepresentativeId);
            }

            if(partnerFunction != null && partnerFunction != "") {
                membershipQueryDto.setPartnerFunction(partnerFunction);
            }

            if(partnerId != null && partnerId != "") {
                membershipQueryDto.setMemberId(partnerId);
            }

            if(idNumber != null && idNumber != "") {
                membershipQueryDto.setIdNumber(idNumber);
            }

            if (dateJoined != null) {

                // Define the formatter for the input string
                SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");

                // Parse the string into Date object
                Date date = formatter.parse(dateJoined);

                membershipQueryDto.setDateJoined(date);


            }

            return ResponseEntity.ok(gson.toJson(membershipService.search(membershipQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMembership(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(membershipService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "filter", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMembershipsByFilters(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String partnerFunction,
                                                     @RequestParam(required = false) String memberId,
                                                     @RequestParam(required = false) String idNumber,
                                                     @RequestParam(required = false) String productId,
                                                     @RequestParam(required = false) String dateJoined) {
        try {
            MembershipQueryDto membershipQueryDto = new MembershipQueryDto();

            if (status != null) {
                membershipQueryDto.setStatus(status);
            }
            if (partnerFunction != null) {
                membershipQueryDto.setPartnerFunction(partnerFunction);
            }
            if (memberId != null) {

                membershipQueryDto.setMemberId(memberId);
            }
            if (idNumber != null) {
                membershipQueryDto.setIdNumber(idNumber);
            }
            if (productId != null) {
                membershipQueryDto.setProductId(productId);
            }
            if (dateJoined != null) {

                membershipQueryDto.setDateJoined(Conversion.stringToDate(dateJoined));
            }
            return ResponseEntity.ok(gson.toJson(membershipService.getByFilter(membershipQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
//    getByFilter
    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editMembership(@PathVariable String id, @RequestBody MembershipEditDto membershipDto) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            TransactionPartnerEdit partnerEdit = new TransactionPartnerEdit();
            boolean edited = false;
            if (membershipDto.getStatus() != null && membershipDto.getStatus() != "") {
                transactionEditDto.setStatus(membershipDto.getStatus());
            }
            if (membershipDto.getStatusReason() != null && membershipDto.getStatusReason() != "") {
                transactionEditDto.setStatusReason(membershipDto.getStatusReason());
            }
            if (transactionEditDto.getStatusReason() != null || transactionEditDto.getStatus() != null) {
                transactionEditDto.setId(id);
                transactionService.edit(transactionEditDto);
            }
            if (membershipDto.getSalesRepresentativeId() != null && membershipDto.getSalesRepresentativeId() != "") {
                partnerEdit.setPartnerFunction(PartnerFunction.SALES_REPRESENTATIVE);
                partnerEdit.setTransaction(id);
                partnerEdit.setParnter(membershipDto.getSalesRepresentativeId());
                edited = transactionService.partnerEdit(partnerEdit);
            }
            if (membershipDto.getPremium() != null && membershipDto.getProductId() != null && membershipDto.getProductId() != "") {
                TransactionItemEditDto editDto = new TransactionItemEditDto();
                editDto.setTransaction(id);
                editDto.setProduct(membershipDto.getProductId());
                editDto.setUnitPrice(membershipDto.getPremium());
                edited = transactionService.editItem(editDto);
            }
            if (membershipDto.getProductId() != null && membershipDto.getProductId() != "" && membershipDto.getPreviousProduct() != null && membershipDto.getPreviousProduct() != "") {
                TransactionItemEditDto editDto = new TransactionItemEditDto();
                editDto.setTransaction(id);
                editDto.setProduct(membershipDto.getProductId());
                editDto.setPreviousProduct(membershipDto.getPreviousProduct());
                edited = transactionService.editItem(editDto);
            }
            return ResponseEntity.ok().body(gson.toJson(edited));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteMembership(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/dependent", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDependent(@PathVariable String id, @RequestBody DependentCreateDto dependentDto) {
        try {
            dependentService.add(dependentDto,id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/dependent", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDependent(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(dependentService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/dependent/{dependentId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDependent(@PathVariable String id, @PathVariable String dependentId) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setFunction(PartnerFunction.DEPENDENT);
            transactionPartnerDto.setPartner(dependentId);
            transactionService.removePartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/claims", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaims(@PathVariable String id) {
        try {
            List<ClaimDto> claimDtoList = new ArrayList<>();
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.CLAIM);
            for (String claimId : transactionService.search(transactionQueryDto)) {
//                claimDtoList.add(getClaimData(transactionDto.getId()));
            }
            return ResponseEntity.ok(gson.toJson(claimDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/tombstone-recipient/{recipientId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addTombstoneRecipient(@PathVariable String id,
                                                   @PathVariable String recipientId) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setPartner(recipientId);
            transactionPartnerDto.setFunction(PartnerFunction.TOMBSTONE_RECIPIENT);
            transactionService.addPartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/tombstone-recipient/{recipientId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeTombstoneRecipient(@PathVariable String id,
                                                      @PathVariable String recipientId) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(id);
            transactionPartnerDto.setPartner(recipientId);
            transactionPartnerDto.setFunction(PartnerFunction.TOMBSTONE_RECIPIENT);
            transactionService.removePartner(transactionPartnerDto);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }

    }

    @RequestMapping(value = "{id}/tombstone-recipient", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getTombstoneRecipient(@PathVariable String id) {
        try {
            List<TombstoneRecipientDto> tombstoneRecipientDtos = new ArrayList<>();
            List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id).stream()
                    .filter(a -> Objects.equals(a.getFunction(), PartnerFunction.TOMBSTONE_RECIPIENT))
                    .toList();
            for (TransactionPartnerDto partnerDtos : transactionPartnerDtoList) {
                PartnerDto partnerDto = partnerService.get(partnerDtos.getPartner());
                if (partnerDto != null) {
                    TombstoneRecipientDto tombstoneRecipient = new TombstoneRecipientDto();
                    tombstoneRecipient.setId(partnerDto.getId());
                    tombstoneRecipient.setFirstName(partnerDto.getName2());
                    tombstoneRecipient.setMiddleName(partnerDto.getName3());
                    tombstoneRecipient.setLastName(partnerDto.getName1());
                    tombstoneRecipient.setGender(partnerDto.getGender().getDescription());
                    tombstoneRecipient.setTitle(partnerDto.getTitle().getDescription());
                    tombstoneRecipientDtos.add(tombstoneRecipient);
                }
            }
            return ResponseEntity.ok(gson.toJson(tombstoneRecipientDtos));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "{id}/activate", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approve(@PathVariable String id,
                                     @RequestParam(required = false) String statusReason,
                                     @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.ACTIVE);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason);
            }
            if (description != null && description != null) {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/de-activate", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @RequestParam(required = false) String statusReason,
                                    @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.INACTIVE);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason);
            }
            if (description != null && description != null) {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/cancel", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancel(@PathVariable String id,
                                    @RequestParam(required = false) String statusReason,
                                    @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.CANCELLED);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason);
            }
            if (description != null && description != null) {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
