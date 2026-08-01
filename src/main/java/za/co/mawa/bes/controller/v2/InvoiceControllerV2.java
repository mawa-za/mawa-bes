package za.co.mawa.bes.controller.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.mawa.bes.dto.InvoiceOutboundDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.service.InvoicePDFService;
import za.co.mawa.bes.service.InvoiceService;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "v2/invoice")
public class InvoiceControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(InvoiceControllerV2.class);

    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private InvoicePDFService invoicePDFService;

    @PostMapping
    public ResponseEntity<?> createInvoice(@RequestBody InvoiceEntity invoice) {
        InvoiceEntity createdInvoice = invoiceService.createInvoice(invoice);
        InvoiceOutboundDto responseDto = invoiceService.mapToDto(createdInvoice);
        return ResponseEntity.ok(responseDto);

    }
    @PutMapping(value = "{id}")
    public ResponseEntity<?> updateInvoice(@PathVariable String id, @RequestBody InvoiceEntity invoice) {
        try {
            return ResponseEntity.ok(invoiceService.mapToDto(invoiceService.updateInvoice(id, invoice)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getInvoices(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String partnerId,
                                         @RequestParam(required = false) String invoiceDate) {
        try {
            return ResponseEntity.ok(invoiceService.searchInvoiceDtos(status, partnerId, invoiceDate));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "code", "INVALID_INVOICE_FILTER",
                    "message", exception.getMessage()));
        } catch (Exception exception) {
            log.error("Unable to retrieve invoices", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "code", "INVOICE_LIST_UNAVAILABLE",
                    "message", "Invoices could not be loaded right now"));
        }
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<?> getInvoice(@PathVariable String id) {
        try {
            return invoiceService.getInvoiceDto(id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of(
                            "code", "INVOICE_NOT_FOUND",
                            "message", "Invoice could not be found")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "code", "INVALID_INVOICE_ID",
                    "message", exception.getMessage()));
        } catch (Exception exception) {
            log.error("Unable to retrieve invoice {}", id, exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "code", "INVOICE_UNAVAILABLE",
                    "message", "The invoice could not be loaded right now"));
        }
    }

    @GetMapping(value = "{id}/lines")
    public ResponseEntity<?> getInvoiceLines(@PathVariable String id) {
        List<InvoiceLineEntity> lines = invoiceService.getInvoiceLines(id);
        return ResponseEntity.ok(lines);
    }

    @GetMapping(value = "{id}/payments")
    public ResponseEntity<?> getInvoicePayments(@PathVariable String id) {
        List<InvoicePaymentEntity> payments = invoiceService.getInvoicePayments(id);
        return ResponseEntity.ok(payments);
    }

    @DeleteMapping(value = "{id}")
    public ResponseEntity<?> deleteInvoice(@PathVariable String id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok("Invoice deleted successfully");
    }

    @PostMapping(value = "{id}/send-to-xero")
    public ResponseEntity<?> sendInvoiceToXero(@PathVariable String id) {
        try {
            InvoiceEntity invoice = invoiceService.queueInvoiceForXero(id);
            return ResponseEntity.ok(invoiceService.mapToDto(invoice));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to queue invoice for Xero: " + exception.getMessage());
        }
    }

    @GetMapping("{id}/pdf")
    public ResponseEntity<ByteArrayResource> generateInvoicePdf(@PathVariable String id) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));

        ByteArrayOutputStream pdfOutput = invoicePDFService.generateInvoicePdf(invoice);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdfOutput.toByteArray()));
    }


}