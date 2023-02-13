package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.sales.order.SalesOrderCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.TransactionType;

@RestController
@CrossOrigin
public class SalesOrderController {
    @Autowired
    TransactionService transactionService;
    Gson gson = new Gson();

    @RequestMapping(value = "/sales-order", method = RequestMethod.POST)
    public ResponseEntity<?> postSalesOrder(@RequestBody SalesOrderCreateDto salesOrderCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.SALES_ORDER);
            return ResponseEntity.ok(gson.toJson(transactionService.create(transactionCreateDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order", method = RequestMethod.GET)
    public ResponseEntity<?> getSalesOrder() {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            return ResponseEntity.ok(gson.toJson(transactionService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getSalesOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editSalesOrder(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteSalesOrder(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
