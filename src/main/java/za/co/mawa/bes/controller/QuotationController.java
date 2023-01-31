package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.QuotationCreateDto;
import za.co.mawa.bes.dto.QuotationDto;
import za.co.mawa.bes.dto.QuotationQueryDto;
import za.co.mawa.bes.service.QuotationService;

@RestController
@CrossOrigin
public class QuotationController {
    @Autowired
    QuotationService quotationService;
    Gson gson = new Gson();

    @RequestMapping(value = "/quotation", method = RequestMethod.POST)
    public ResponseEntity<?> postQuotation(@RequestBody QuotationCreateDto quotationCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(quotationService.create(quotationCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation", method = RequestMethod.GET)
    public ResponseEntity<?> getQuotations(@RequestBody QuotationQueryDto quotationQueryDto) {
        try {
            return ResponseEntity.ok(gson.toJson(quotationService.search(quotationQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getQuotation(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(quotationService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editQuotation(@PathVariable String id, @RequestBody QuotationDto quotationDto) {
        try {
            return ResponseEntity.ok(gson.toJson(quotationService.edit(quotationDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteQuotation(@PathVariable String id) {
        try {
            quotationService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
