package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.CustomerStatementService;

import java.time.LocalDate;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/v2/customer-statement")
public class CustomerStatementControllerV2 {
    private final CustomerStatementService customerStatementService;

    @GetMapping("/{partnerId}")
    public ResponseEntity<?> statement(@PathVariable String partnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try { return ResponseEntity.ok(customerStatementService.generate(partnerId, fromDate, toDate)); }
        catch (Exception exception) { return ResponseEntity.badRequest().body(exception.getMessage()); }
    }

    // Do not constrain handler selection using `produces`. Older ERP clients
    // sent Accept: application/json even though they expected PDF bytes, which
    // caused Spring to reject the request before this method was invoked.
    @GetMapping("/{partnerId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String partnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        byte[] pdf = customerStatementService.generatePdf(partnerId, fromDate, toDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=customer-statement-" + partnerId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
