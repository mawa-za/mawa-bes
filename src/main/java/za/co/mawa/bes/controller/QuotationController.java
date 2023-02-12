package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.service.QuotationService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.TransactionType;

@RestController
@CrossOrigin
public class QuotationController {
    @Autowired
    TransactionService transactionService;
    Gson gson = new Gson();

    @RequestMapping(value = "/quotation", method = RequestMethod.POST)
    public ResponseEntity<?> postQuotation(@RequestBody QuotationCreateDto quotationCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.QUOTATION);
            return ResponseEntity.ok(gson.toJson(transactionService.create(transactionCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation", method = RequestMethod.GET)
    public ResponseEntity<?> getQuotations() {
        try {
            QuotationQueryDto quotationQueryDto = new QuotationQueryDto();
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            return ResponseEntity.ok(gson.toJson(transactionService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getQuotation(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editQuotation(@PathVariable String id, @RequestBody QuotationDto quotationDto) {
        try {
            transactionService.edit(quotationDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteQuotation(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}
