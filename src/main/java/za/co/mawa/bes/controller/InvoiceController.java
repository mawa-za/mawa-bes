package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.TransactionCreateDto;
import za.co.mawa.bes.dto.TransactionDto;
import za.co.mawa.bes.dto.TransactionQueryDto;
import za.co.mawa.bes.service.InvoiceService;
import za.co.mawa.bes.service.QuotationService;

@RestController
@CrossOrigin
public class InvoiceController {
    @Autowired
    InvoiceService invoiceService;
    Gson gson = new Gson();

    @RequestMapping(value = "/invoice", method = RequestMethod.POST)
    public ResponseEntity<?> postInvoice(@RequestBody TransactionCreateDto transactionCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(invoiceService.create(transactionCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/invoice", method = RequestMethod.GET)
    public ResponseEntity<?> getInvoices(@RequestBody TransactionQueryDto transactionQueryDto) {
        try {
            return ResponseEntity.ok(gson.toJson(invoiceService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/invoice/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getInvoice(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(invoiceService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/invoice/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editInvoice(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            invoiceService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/invoice/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteInvoice(@PathVariable String id) {
        try {
            invoiceService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
