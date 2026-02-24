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
import za.co.mawa.bes.service.ReceiptRangeService;

@RestController
@CrossOrigin
@RequestMapping(value = "receipt-range")
public class ReceiptRangeController {
    Gson gson = new Gson();
    private final ReceiptRangeService service;

    public ReceiptRangeController(ReceiptRangeService service) {
        this.service = service;
    }

    @PostMapping("/allocate")
    public ResponseEntity<ReceiptRangeService.AllocateResponse> allocate(
            @RequestBody ReceiptRangeService.AllocateRequest req
    ) {
        return ResponseEntity.ok(service.allocate(req));
    }
}
