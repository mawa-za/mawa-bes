package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.service.ClaimService;
import za.co.mawa.bes.service.QuotationService;

@RestController
@CrossOrigin
public class ClaimController {
    @Autowired
    ClaimService claimService;
    Gson gson = new Gson();

    @RequestMapping(value = "/claim", method = RequestMethod.POST)
    public ResponseEntity<?> postClaim(@RequestBody ClaimCreateDto claimCreateDto) {

        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto(claimCreateDto);
            return ResponseEntity.ok(gson.toJson(claimService.create(transactionCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim", method = RequestMethod.GET)
    public ResponseEntity<?> getClaims(@RequestBody ClaimQueryDto claimQueryDto) {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto(claimQueryDto);
            return ResponseEntity.ok(gson.toJson(claimService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getClaim(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(claimService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editClaim(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            claimService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteClaim(@PathVariable String id) {
        try {
            claimService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
