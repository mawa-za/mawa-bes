package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.numbering.NumberAllocationRequest;
import za.co.mawa.bes.dto.v2.numbering.NumberAllocationResponse;
import za.co.mawa.bes.service.v2.NumberAllocationService;

@RestController
@RequestMapping("/v2/number-allocation")
public class NumberAllocationControllerV2 {

    private final NumberAllocationService numberAllocationService;

    public NumberAllocationControllerV2(NumberAllocationService numberAllocationService) {
        this.numberAllocationService = numberAllocationService;
    }

    @PostMapping("/allocate")
    public ResponseEntity<NumberAllocationResponse> allocate(
            @RequestBody NumberAllocationRequest request
    ) {
        return ResponseEntity.ok(numberAllocationService.allocate(request));
    }

    @GetMapping("/active")
    public ResponseEntity<NumberAllocationResponse> getLatestActiveRange(
            @RequestParam String deviceId,
            @RequestParam String seqType
    ) {
        return ResponseEntity.ok(
                numberAllocationService.getLatestActiveRange(deviceId, seqType)
        );
    }
}