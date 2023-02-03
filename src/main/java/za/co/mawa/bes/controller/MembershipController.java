package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.TransactionCreateDto;
import za.co.mawa.bes.dto.TransactionDto;
import za.co.mawa.bes.dto.TransactionQueryDto;
import za.co.mawa.bes.service.MembershipService;
import za.co.mawa.bes.service.QuotationService;

@RestController
@CrossOrigin
public class MembershipController {
    @Autowired
    MembershipService membershipService;
    Gson gson = new Gson();

    @RequestMapping(value = "/membership", method = RequestMethod.POST)
    public ResponseEntity<?> postMembership(@RequestBody TransactionCreateDto transactionCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(membershipService.create(transactionCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/membership", method = RequestMethod.GET)
    public ResponseEntity<?> getMemberships(@RequestBody TransactionQueryDto transactionQueryDto) {
        try {
            return ResponseEntity.ok(gson.toJson(membershipService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/membership/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getMembership(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(membershipService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/membership/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editMembership(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            membershipService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/membership/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteMembership(@PathVariable String id) {
        try {
            membershipService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
