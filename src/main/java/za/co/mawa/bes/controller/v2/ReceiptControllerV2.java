package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.PrintJobResponse;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.QueueReceiptRequest;
import za.co.mawa.bes.dto.v2.ReceiptPrintDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.service.v2.PosPrintingService;
import za.co.mawa.bes.service.v2.ReceiptService;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("v2/receipts")
public class ReceiptControllerV2 {

    private final @Qualifier("ReceiptServiceV2") ReceiptService receiptService;
    private final PosPrintingService posPrintingService;

    @GetMapping("/{receiptId}")
    public ReceiptResponseDto getReceipt(@PathVariable String receiptId) {
        return receiptService.getReceipt(receiptId);
    }

    @GetMapping("/by-number/{receiptNo}")
    public ReceiptResponseDto getReceiptByNumber(@PathVariable String receiptNo) {
        return receiptService.getReceiptByNumber(receiptNo);
    }

    @GetMapping("/{receiptId}/print")
    public ReceiptPrintDto getPrintData(@PathVariable String receiptId) {
        return receiptService.previewPrintData(receiptId);
    }

    @PostMapping("/{receiptId}/print-jobs")
    public PrintJobResponse queuePrint(
            @PathVariable String receiptId,
            @RequestBody QueueReceiptRequest request
    ) {
        return posPrintingService.queueReceipt(receiptId, request);
    }

    @PostMapping("/{receiptId}/direct-print-spooled")
    public void confirmDirectPrint(@PathVariable String receiptId) {
        receiptService.recordSpooledPrint(receiptId);
    }

    @PostMapping("/{receiptId}/reprint")
    public ReceiptPrintDto reprint(@PathVariable String receiptId) {
        return receiptService.previewPrintData(receiptId);
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
