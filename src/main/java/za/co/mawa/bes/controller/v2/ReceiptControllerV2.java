package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
        import za.co.mawa.bes.dto.v2.ReceiptPrintDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.service.v2.ReceiptService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/receipts")
public class ReceiptControllerV2 {

    private final ReceiptService receiptService;

    @GetMapping("/{receiptId}")
    public ReceiptResponseDto getReceipt(
            @PathVariable String receiptId
    ) {
        return receiptService.getReceipt(receiptId);
    }

    @GetMapping("/by-number/{receiptNo}")
    public ReceiptResponseDto getReceiptByNumber(
            @PathVariable String receiptNo
    ) {
        return receiptService.getReceiptByNumber(receiptNo);
    }

    @GetMapping("/{receiptId}/print")
    public ReceiptPrintDto getPrintData(
            @PathVariable String receiptId
    ) {
        return receiptService.getPrintData(receiptId);
    }

    @PostMapping("/{receiptId}/reprint")
    public ReceiptPrintDto reprint(
            @PathVariable String receiptId
    ) {
        return receiptService.getPrintData(receiptId);
    }

    @PostMapping("/{receiptId}/reverse")
    public ReceiptResponseDto reverseReceipt(
            @PathVariable String receiptId,
            @RequestParam String reason,
            @RequestParam String reversedBy
    ) {
        return receiptService.reverseReceipt(receiptId, reason, reversedBy);
    }
}