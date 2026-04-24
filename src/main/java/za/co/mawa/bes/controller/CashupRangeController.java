package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.CashupRangeService;
import za.co.mawa.bes.service.ReceiptRangeService;

@RestController
@CrossOrigin
@RequestMapping(value = "cashup-range")
public class CashupRangeController {
    Gson gson = new Gson();
    private final CashupRangeService service;

    public CashupRangeController(CashupRangeService service) {
        this.service = service;
    }

    @PostMapping("/allocate")
    public ResponseEntity<CashupRangeService.AllocateResponse> allocate(
            @RequestBody CashupRangeService.AllocateRequest req
    ) {
        return ResponseEntity.ok(service.allocate(req));
    }
}
