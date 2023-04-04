package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.service.ReceiptService;

@RestController
@CrossOrigin
public class ReceiptController {
    Gson gson = new Gson();

    @Autowired
    ReceiptService receiptService;

    @RequestMapping(value= "/receipt" , method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createReceipt(@RequestBody ReceiptCreateDto receiptCreateDto) {
        try {
            ReceiptDto receiptDto = receiptService.createReceipt(receiptCreateDto);
            return ResponseEntity.ok(gson.toJson(receiptDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }
}
