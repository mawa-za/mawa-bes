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

    @GetMapping(value = "/{partnerId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable String partnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer-statement-" + partnerId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(customerStatementService.generatePdf(partnerId, fromDate, toDate));
    }
}
