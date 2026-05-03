package za.co.mawa.bes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.InvoiceOutboundDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.service.InvoiceService;

import java.util.ArrayList;
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
        InvoiceOutboundDto responseDto = invoiceService.mapToDto(createdInvoice);
        return ResponseEntity.ok(responseDto);

    }
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getInvoices(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String partnerId,
                                         @RequestParam(required = false) String invoiceDate) {
        try {
            List<InvoiceEntity> invoices;
            List<InvoiceOutboundDto> invoiceOutboundDtoList = new ArrayList<>();

            // Check and apply filters if specified
            if (status != null && !status.isEmpty()) {
                invoices = invoiceService.getInvoicesByStatus(status);
            } else if (partnerId != null && !partnerId.isEmpty()) {
                invoices = invoiceService.getInvoicesByPartnerId(partnerId);
            } else if (invoiceDate != null && !invoiceDate.isEmpty()) {
                invoices = invoiceService.getInvoicesByDate(invoiceDate);
            } else {
                invoices = invoiceService.getAllInvoices(); // Fetch all invoices if no filters are provided
            }
            for (InvoiceEntity invoice : invoices) {
                InvoiceOutboundDto invoiceOutboundDto = invoiceService.mapToDto(invoice);
                invoiceOutboundDtoList.add(invoiceOutboundDto);
            }
            return ResponseEntity.ok(invoiceOutboundDtoList);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error retrieving invoices: " + exception.getMessage());
        }
    }

    @GetMapping(value = "{id}")
    public ResponseEntity<?> getInvoice(@PathVariable String id) {
        Optional<InvoiceEntity> invoice = invoiceService.getInvoice(id);
        if (invoice.isPresent()) {
            InvoiceOutboundDto invoiceOutboundDto = invoiceService.mapToDto(invoice.get());
            return ResponseEntity.ok(invoiceOutboundDto);
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

    @PostMapping("/{id}/pdf/base64")
    public ResponseEntity<String> generateInvoicePdfBase64(@PathVariable String id) {
        // Call the service to generate the Base64 string
        String base64Pdf = invoiceService.generateInvoicePdfAsBase64(id);

        // Return the Base64 PDF wrapped as plain text
        return ResponseEntity.ok(base64Pdf);
    }

}