package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.ReceiptRangeInboundDto;
import za.co.mawa.bes.dto.ReceiptRangeOutboundDto;
import za.co.mawa.bes.dto.receipt.ReceiptCreateDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;

@RestController
@CrossOrigin
@RequestMapping(value = "receipt-range")
public class ReceiptRangeController {
    Gson gson = new Gson();
    @RequestMapping(value = "allocate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> allocateRange(@RequestBody ReceiptRangeInboundDto receiptRangeInboundDto) {
        try {
            ReceiptRangeOutboundDto receiptRangeOutboundDto = new ReceiptRangeOutboundDto();
            receiptRangeOutboundDto.setRangeId("RNG-00045");
            receiptRangeOutboundDto.setFromNo(1000000000);
            receiptRangeOutboundDto.setFromNo(1000000999);
            receiptRangeOutboundDto.setFromNo(1000000000);
            return ResponseEntity.ok(gson.toJson(receiptRangeOutboundDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }

    }
}
