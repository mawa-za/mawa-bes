package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.creditnote.CreditNoteIssueRequestDto;
import za.co.mawa.bes.service.CreditNoteService;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/v2")
public class CreditNoteControllerV2 {
    private final CreditNoteService creditNoteService;

    @PostMapping(value = "/invoice/{invoiceId}/credit-note", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> issue(@PathVariable String invoiceId, @RequestBody CreditNoteIssueRequestDto request,
                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try { return ResponseEntity.status(HttpStatus.CREATED).body(creditNoteService.issue(invoiceId, request, userId)); }
        catch (Exception exception) { return ResponseEntity.badRequest().body(exception.getMessage()); }
    }

    @GetMapping("/invoice/{invoiceId}/credit-notes")
    public ResponseEntity<?> list(@PathVariable String invoiceId) {
        try { return ResponseEntity.ok(creditNoteService.findByInvoice(invoiceId)); }
        catch (Exception exception) { return ResponseEntity.badRequest().body(exception.getMessage()); }
    }

    @GetMapping(value = "/credit-note/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable String id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=credit-note-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(creditNoteService.generatePdf(id));
    }
}
