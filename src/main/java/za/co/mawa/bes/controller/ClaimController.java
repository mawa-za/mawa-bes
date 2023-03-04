package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.claim.ClaimCreateDto;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.PartnerNotFound;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.ClaimStatus;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
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

    @RequestMapping(value = "/claim", method = RequestMethod.POST)
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
            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim", method = RequestMethod.GET)
    public ResponseEntity<?> getClaims() {
        try {
            List<ClaimDto> claimDtoList = new ArrayList<>();
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.CLAIM);
            for (TransactionQueryResultDto transactionDto : transactionService.search(transactionQueryDto)) {
                claimDtoList.add(getClaimData(transactionDto.getId()));
            }
            return ResponseEntity.ok(gson.toJson(claimDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getClaim(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(getClaimData(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}/approve", method = RequestMethod.PUT)
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

    @RequestMapping(value = "/claim/{id}/reject", method = RequestMethod.PUT)
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

    @RequestMapping(value = "/claim/{id}/dispute", method = RequestMethod.PUT)
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

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editClaim(@PathVariable String id, @RequestBody ClaimDto claimDto) {
        try {
            TransactionDto transactionDto = new TransactionDto();
            transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteClaim(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private ClaimDto getClaimData(String id) throws TransactionNotFound {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            ClaimDto claimDto = new ClaimDto();
            claimDto.setId(transactionDto.getId());
            claimDto.setNumber(transactionDto.getNumber());
            claimDto.setType(transactionDto.getSubType());
            claimDto.setStatus(transactionDto.getStatus());
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.MAINMEMBER)) {
                    claimDto.setMemberId(transactionPartnerDto.getPartner());
                }
                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.DECEASED)) {
                    claimDto.setDeceasedId(transactionPartnerDto.getPartner());
                }
                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CLAIMANT)) {
                    claimDto.setDeceasedId(transactionPartnerDto.getPartner());
                }
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (Objects.equals(transactionDateDto.getType(), DateType.CREATED)) {
                    claimDto.setCreationDate(transactionDateDto.getValue());
                }
            }
            return claimDto;
        }catch(TransactionNotFound exception){
            throw new TransactionNotFound("Claim not found");
        }
    }
}
