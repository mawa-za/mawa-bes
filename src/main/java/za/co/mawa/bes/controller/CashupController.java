package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.cashup.CashupCreateDto;
import za.co.mawa.bes.dto.cashup.CashupDto;
import za.co.mawa.bes.dto.cashup.CashupEditDto;
import za.co.mawa.bes.dto.cashup.CashupSearchDto;
import za.co.mawa.bes.dto.claim.ClaimCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryResultDto;
import za.co.mawa.bes.service.CashupService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.TransactionType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@RestController
@CrossOrigin
public class CashupController {
    Gson gson = new Gson();
    @Autowired
    CashupService cashupService;
    @Autowired
    TransactionService transactionService;
    @RequestMapping(value = "/cashup", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postCashUp(@RequestBody CashupCreateDto cashupCreateDto) {
        try{
            CashupDto cashup = new CashupDto();
            String cashUpType;
            cashUpType = String.valueOf(cashupCreateDto.getCashUpType()).toUpperCase();

            if(cashUpType.equals("RECEIPT")) {

                String id = cashupService.create(cashupCreateDto);
                if (id != null) {
                    cashup.setId(id);
                } else {
                    throw new RuntimeException("Failed to create cashup");
                }
            }

            if(cashUpType.equals("MANUAL")){

                String id = cashupService.createManualCashUp(cashupCreateDto);
                if (id != null) {
                    cashup.setId(id);
                } else {
                    throw new RuntimeException("Failed to create cashup");
                }
            }

            return ResponseEntity.ok().body(gson.toJson(cashup));

    } catch (Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
       }
    }
    @RequestMapping(value = "/cashup/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCashUp(@PathVariable String id) {
        try{

            return ResponseEntity.ok().body(gson.toJson(cashupService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/cashup", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCashUps(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String createDate,
                                        @RequestParam(required = false) String lastUpdated,
                                        @RequestParam(required = false) String createdBy,
                                        @RequestParam(required = false) String cashUpType,
                                        @RequestParam(required = false) String changedBy) {
        try{
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            ArrayList<CashupDto> cashups = new ArrayList<>();
            transactionQueryDto.setType(TransactionType.CASHUP);
            if(cashUpType != null && cashUpType != ""){

                transactionQueryDto.setSubtype(cashUpType);
            }
            if(status != null && status != "")
            {
                transactionQueryDto.setStatus(status);
            }
            if(createDate != null && createDate != "")
            {
                Date create = new SimpleDateFormat("yyyy-MM-dd").parse(createDate);
                transactionQueryDto.setValue(create);
                transactionQueryDto.setDateType(DateType.CREATED);
            }
            if(lastUpdated != null && lastUpdated != "")
            {
                Date updated = new SimpleDateFormat("yyyy-MM-dd").parse(lastUpdated);
                transactionQueryDto.setValue(updated);
                transactionQueryDto.setDateType(DateType.LAST_UPDATED);
            }
            if(createdBy != null && createdBy != "")
            {
                transactionQueryDto.setCreatedBy(createdBy);
            }
            if(changedBy != null && changedBy != "")
            {
                transactionQueryDto.setChangedBy(changedBy);
            }
            for(String id : transactionService.search(transactionQueryDto))
            {
                CashupDto cashupDto = cashupService.get(id);

                if (cashupDto != null) {
                    cashups.add(cashupDto);
                }

            }
            return ResponseEntity.ok().body(gson.toJson(cashups));

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
    @RequestMapping(value = "/cashup/{id}", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editCashup(@PathVariable String id,@RequestBody CashupEditDto editDto) {
        try{
            return ResponseEntity.ok().body(gson.toJson(cashupService.edit(editDto,id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
