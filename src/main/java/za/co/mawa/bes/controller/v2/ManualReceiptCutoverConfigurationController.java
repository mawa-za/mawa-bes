package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.ManualReceiptCutoverConfigurationDto;
import za.co.mawa.bes.service.v2.ManualReceiptCutoverConfigurationService;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/manual-receipts/configuration")
public class ManualReceiptCutoverConfigurationController {
    private final ManualReceiptCutoverConfigurationService service;
    @GetMapping public ManualReceiptCutoverConfigurationDto get() { return service.get(); }
    @PutMapping public ManualReceiptCutoverConfigurationDto save(@RequestBody ManualReceiptCutoverConfigurationDto dto) { return service.save(dto); }
}
