package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.ClaimCancelDto;
import za.co.mawa.bes.dto.ClaimDisputeDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.claim.ClaimCreateDto;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.claim.ClaimEditDto;
import za.co.mawa.bes.dto.claim.ClaimQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.service.ClaimService;
import za.co.mawa.bes.service.MembershipService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.ClaimStatus;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin
@RequestMapping(value = "claim")
public class ClaimController {
    @Autowired
    ClaimService claimService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    MembershipService membershipService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postClaim(@RequestBody ClaimCreateDto claimCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(claimService.create(claimCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaims(@RequestParam(required = false) String no,
                                       @RequestParam(required = false) String claimant,
                                       @RequestParam(required = false) String deceased,
                                       @RequestParam(required = false) String member,
                                       @RequestParam(required = false) String membership,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String deathDate,
                                       @RequestParam(required = false) String burialDate,
                                       @RequestParam(required = false) String status) {
        try {

            ClaimQueryDto claimQueryDto = new ClaimQueryDto();
            if (status != null && status != "") {
                claimQueryDto.setStatus(status);
            }
            if (no != null && no != "") {
                claimQueryDto.setNumber(no);
            }
            if (claimant != null & claimant != "") {
                claimQueryDto.setClaimant(claimant);
            }
            if (deceased != null && deceased != "") {
                claimQueryDto.setDeceased(deceased);
            }
            if (member != null && member != "") {
                claimQueryDto.setMember(member);
            }
            if (type != null && type != "") {
                claimQueryDto.setType(type);
            }
            if (deathDate != null && deathDate != "") {
                Date death = new SimpleDateFormat("yyyy-MM-dd").parse(deathDate);
                claimQueryDto.setDeathDate(death);
            }
            if (burialDate != null && burialDate != "") {
                Date burial = new SimpleDateFormat("yyyy-MM-dd").parse(burialDate);
                claimQueryDto.setBurialDate(burial);
            }
            if (membership != null && membership != "") {
                claimQueryDto.setMembership(membership);
            }
            return ResponseEntity.ok(gson.toJson(claimService.search(claimQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaim(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(claimService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/claimByMember/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaimByMember(@PathVariable String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            List<ClaimDto> claimDtoList = new ArrayList<>();

            for (TransactionLinkDto transactionLinkDto : transactionService.getLinks(id)) {
                claimDtoList.add(claimService.get(transactionLinkDto.getTransaction2()));
                transactionDto.setClaimDtoList(claimDtoList);
            }
            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/approve", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approveClaim(@PathVariable String id,
                                          @RequestParam(required = false) String statusReason,
                                          @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(ClaimStatus.APPROVED);
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

    @RequestMapping(value = "{id}/reject", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rejectClaim(@PathVariable String id,
                                         @RequestParam(required = true) String statusReason,
                                         @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(ClaimStatus.REJECTED);
            transactionEditDto.setStatusReason(statusReason);
            if (description != null && description != "") {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/submit", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submitClaim(@PathVariable String id) {
        try {
            claimService.submit(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "{id}/dispute", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> disputeClaim(@PathVariable String id,
                                          @RequestBody ClaimDisputeDto dispute) {
        try {
            dispute.setClaimId(id);
            claimService.dispute(dispute);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/cancel", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancelClaim(@PathVariable String id,
                                          @RequestBody ClaimCancelDto claimCancelDto) {
        try {
            claimCancelDto.setClaimId(id);
            claimService.cancel(claimCancelDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editClaim(@PathVariable String id, @RequestBody ClaimEditDto claimDto) {
        try {
            boolean edited = false;
            if (claimDto.getBurialDate() != null) {
                TransactionDateEdit transactionDateEdit = new TransactionDateEdit();
                transactionDateEdit.setTransaction(id);
                transactionDateEdit.setType(DateType.BURIAL_DATE);
                transactionDateEdit.setValue(claimDto.getBurialDate());
                edited = transactionService.dateEdit(transactionDateEdit);

            }
            if (claimDto.getDeathDate() != null) {
                TransactionDateEdit edit = new TransactionDateEdit();
                edit.setTransaction(id);
                edit.setType(DateType.DEATH_DATE);
                edit.setValue(claimDto.getDeathDate());
                edited = transactionService.dateEdit(edit);

            }
            if (claimDto.getClaimantId() != null) {
                TransactionPartnerEdit edit = new TransactionPartnerEdit();
                edit.setTransaction(id);
                edit.setParnter(claimDto.getClaimantId());
                edit.setPartnerFunction(PartnerFunction.CLAIMANT);
                edited = transactionService.partnerEdit(edit);
            }
            return ResponseEntity.ok(gson.toJson(edited));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteClaim(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/bankDetails", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postClaimBankDetails(@RequestBody TransactionAccountDto transactionAccountDto) {
        try {
            transactionService.addBankAccount(transactionAccountDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    private String getUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }

}
