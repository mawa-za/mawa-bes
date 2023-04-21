package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.claim.ClaimCreateDto;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.claim.ClaimEditDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.PartnerNotFound;
import za.co.mawa.bes.exception.TransactionNotFound;
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
public class ClaimController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    PartnerService partnerService;
    Gson gson = new Gson();

    @RequestMapping(value = "/claim", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postClaim(@RequestBody ClaimCreateDto claimCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.CLAIM);
            transactionCreateDto.setSubType(claimCreateDto.getType());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            transactionService.addDate(creationDate);

            if (claimCreateDto.getMemberId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.MAINMEMBER);
                transactionPartnerDto.setPartner(claimCreateDto.getMemberId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (claimCreateDto.getDeceasedId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.DECEASED);
                transactionPartnerDto.setPartner(claimCreateDto.getDeceasedId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (claimCreateDto.getClaimantId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CLAIMANT);
                transactionPartnerDto.setPartner(claimCreateDto.getClaimantId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if(claimCreateDto.getMembershipId() != null)
            {
               TransactionLinkDto transactionLinkDto = new TransactionLinkDto();

                transactionLinkDto.setTransaction1(claimCreateDto.getMembershipId());
                transactionLinkDto.setTransaction2(transactionDto.getId());
                transactionLinkDto.setType(TransactionType.CLAIM);
//               transactionLinkDto.setCreationDate(new Date());
                transactionLinkDto.setCreateBy(getUser());
                transactionService.addLink(transactionLinkDto);
            }
            if(claimCreateDto.getDeathDate() != null)
            {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.DEATH_DATE);
                transactionDateDto.setValue(claimCreateDto.getDeathDate());
                transactionService.addDate(transactionDateDto);
            }
            if(claimCreateDto.getBurialDate() != null)
            {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.BURIAL_DATE);
                transactionDateDto.setValue(claimCreateDto.getBurialDate());
                transactionService.addDate(transactionDateDto);
            }

            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/claim", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
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
            List<ClaimDto> claimDtoList = new ArrayList<>();
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            if(status != null && status != "") {
                transactionQueryDto.setStatus(status);
            }
            if(no != null && no != "") {
                transactionQueryDto.setNumber(no);
            }
            if(claimant != null & claimant != "") {
                transactionQueryDto.setPartnerNo(claimant);
                transactionQueryDto.setPartnerFunction(PartnerFunction.CLAIMANT);
            }
            if(deceased != null && deceased != ""){
                transactionQueryDto.setPartnerNo(deceased);
                transactionQueryDto.setPartnerFunction(PartnerFunction.DECEASED);
            }
            if(member != null && member != ""){
                transactionQueryDto.setPartnerNo(member);
                transactionQueryDto.setPartnerFunction(PartnerFunction.MAINMEMBER);
            }
            if(type != null && type != "") {
                transactionQueryDto.setSubtype(type);
            }
            if(deathDate != null && deathDate != "") {
                Date death = new SimpleDateFormat("yyyy-MM-dd").parse(deathDate);
                transactionQueryDto.setValue(death);
                transactionQueryDto.setDateType(DateType.DEATH_DATE);
            }
            if(burialDate != null && burialDate != "") {
                Date burial = new SimpleDateFormat("yyyy-MM-dd").parse(burialDate);
                transactionQueryDto.setValue(burial);
                transactionQueryDto.setDateType(DateType.BURIAL_DATE);
            }
            if(membership != null && membership != "") {
              transactionQueryDto.setTransactionlink1(membership);
            }
            transactionQueryDto.setType(TransactionType.CLAIM);
            for (TransactionQueryResultDto transactionDto : transactionService.search(transactionQueryDto)) {
                claimDtoList.add(getClaimData(transactionDto.getId()));
            }
            return ResponseEntity.ok(gson.toJson(claimDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaim(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(getClaimData(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/claimByMember/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getClaimByMember(@PathVariable String id) {
        try {
           TransactionDto transactionDto =  transactionService.get(id);
            List<ClaimDto> claimDtoList = new ArrayList<>();


            for (TransactionLinkDto transactionLinkDto : transactionService.getLinks(id)) {
                  claimDtoList.add(getClaimData(transactionLinkDto.getTransaction2()));
                transactionDto.setClaimDtoList(claimDtoList);
            }
            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/claim/{id}/approve", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approveClaim(@PathVariable String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            transactionDto.setStatus(ClaimStatus.APPROVED);
            transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}/reject", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rejectClaim(@PathVariable String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            transactionDto.setStatus(ClaimStatus.REJECTED);
            transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}/dispute", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> disputeClaim(@PathVariable String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            transactionDto.setStatus(ClaimStatus.DISPUTED);
            transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editClaim(@PathVariable String id, @RequestBody ClaimEditDto claimDto) {
        try {
            boolean edited = false;
            if(claimDto.getBurialDate() != null)
            {
                TransactionDateEdit edit = new TransactionDateEdit();
                edit.setTransaction(id);
                edit.setType(DateType.BURIAL_DATE);
                edit.setValue(claimDto.getBurialDate());
                edited = transactionService.dateEdit(edit);

            }
            if(claimDto.getDeathDate() != null)
            {
                TransactionDateEdit edit = new TransactionDateEdit();
                edit.setTransaction(id);
                edit.setType(DateType.DEATH_DATE);
                edit.setValue(claimDto.getDeathDate());
                edited = transactionService.dateEdit(edit);

            }
            if(claimDto.getClaimantId() != null)
            {
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

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteClaim(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    private String getUser()
    {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }
    private ClaimDto getClaimData(String id) throws TransactionNotFound {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            ClaimDto claimDto = new ClaimDto();
            claimDto.setId(transactionDto.getId());
            claimDto.setNumber(transactionDto.getNumber());
            claimDto.setType(transactionDto.getSubType());
            claimDto.setStatus(transactionDto.getStatus());
            if(transactionDto.getCreatedBy() != null) {
                claimDto.setCreatedBy(transactionDto.getCreatedBy());
            }

            TransactionLinkEntity transactionLink = transactionService.getTransaction(TransactionType.CLAIM,id);
            if(transactionLink != null)
            {
                claimDto.setMembershipId(transactionLink.getTransactionLinkPKEntity().getTransaction1());
            }
            try {
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.MAINMEMBER)) {
                    claimDto.setMemberId(transactionPartnerDto.getPartner());
                    if(partnerService.getPartner(transactionPartnerDto.getPartner()) != null) {
                        claimDto.setMember((new PersonDto(partnerService.getPartner(transactionPartnerDto.getPartner()))));
                    }
                }
                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.DECEASED)) {
                    claimDto.setDeceasedId(transactionPartnerDto.getPartner());
                    if(partnerService.getPartner(transactionPartnerDto.getPartner()) != null) {
                        claimDto.setDeceased((new PersonDto(partnerService.getPartner(transactionPartnerDto.getPartner()))));
                    }
                }
                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CLAIMANT)) {
                    claimDto.setClaimantId(transactionPartnerDto.getPartner());
                    if(partnerService.getPartner(transactionPartnerDto.getPartner()) != null) {
                        claimDto.setClaimant((new PersonDto(partnerService.getPartner(transactionPartnerDto.getPartner()))));
                    }
                }
            }
               }catch(Exception e)
             {
                   throw new RuntimeException(e);
             }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (Objects.equals(transactionDateDto.getType(), DateType.CREATED)) {
                    claimDto.setCreationDate(transactionDateDto.getValue());
                }
                if(Objects.equals(transactionDateDto.getType(), DateType.BURIAL_DATE))
                {
                    claimDto.setBurialDate(transactionDateDto.getValue());
                }
                if(Objects.equals(transactionDateDto.getType(), DateType.DEATH_DATE))
                {
                    claimDto.setDeathDate(transactionDateDto.getValue());
                }
            }

            return claimDto;
        }catch(TransactionNotFound exception){
            throw new RuntimeException(new TransactionNotFound("Claim not found"));
        }
    }
}
