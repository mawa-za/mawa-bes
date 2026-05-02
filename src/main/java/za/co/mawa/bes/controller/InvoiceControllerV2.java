package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.service.InvoiceService;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping(value = "v2/invoice")
public class InvoiceControllerV2 {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<?> createInvoice(@RequestBody InvoiceEntity invoice) {
        InvoiceEntity createdInvoice = invoiceService.createInvoice(invoice);
        return ResponseEntity.ok(createdInvoice);
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<?> getInvoice(@PathVariable String id) {
        Optional<InvoiceEntity> invoice = invoiceService.getInvoice(id);
        if (invoice.isPresent()) {
            return ResponseEntity.ok(invoice.get());
        } else {
            return ResponseEntity.status(404).body("Invoice not found");
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
}