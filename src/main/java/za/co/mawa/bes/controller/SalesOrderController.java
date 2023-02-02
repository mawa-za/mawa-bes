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
import za.co.mawa.bes.service.SalesOrderService;
import za.co.mawa.bes.service.ServiceRequestService;

@RestController
@CrossOrigin
public class SalesOrderController {
    @Autowired
    SalesOrderService salesOrderService;
    Gson gson = new Gson();

    @RequestMapping(value = "/sales-order", method = RequestMethod.POST)
    public ResponseEntity<?> postSalesOrder(@RequestBody TransactionCreateDto transactionCreateDto) {
        try {
            return ResponseEntity.ok(gson.toJson(salesOrderService.create(transactionCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order", method = RequestMethod.GET)
    public ResponseEntity<?> getSalesOrder(@RequestBody TransactionQueryDto transactionQueryDto) {
        try {
            return ResponseEntity.ok(gson.toJson(salesOrderService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getSalesOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(salesOrderService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editSalesOrder(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            salesOrderService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteSalesOrder(@PathVariable String id) {
        try {
            salesOrderService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
