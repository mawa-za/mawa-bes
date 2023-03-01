package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.sales.order.SalesOrderCreateDto;
import za.co.mawa.bes.dto.sales.order.SalesOrderEditDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.util.Date;

@RestController
@CrossOrigin
public class SalesOrderController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    Gson gson = new Gson();

    @RequestMapping(value = "/sales-order", method = RequestMethod.POST)
    public ResponseEntity<?> postSalesOrder(@RequestBody SalesOrderCreateDto salesOrderCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.SALES_ORDER);
            transactionCreateDto.setDescription(salesOrderCreateDto.getDescription());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            creationDate.setValue(new Date());
            transactionService.addDate(creationDate);

            if (salesOrderCreateDto.getExpectedDate() != null) {
                TransactionDateDto expectedDate = new TransactionDateDto();
                creationDate.setTransaction(transactionDto.getId());
                creationDate.setType(DateType.DATE_EXPECTED);
                creationDate.setValue(new Date());
                transactionService.addDate(expectedDate);
            }

            if (salesOrderCreateDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(salesOrderCreateDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (salesOrderCreateDto.getSalesRepresentativeId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
                transactionPartnerDto.setPartner(salesOrderCreateDto.getSalesRepresentativeId());
                transactionService.addPartner(transactionPartnerDto);
            }
            for (LineItemDto item: salesOrderCreateDto.getItems()) {
                ProductDto productDto = productService.get(item.getProductId());
                TransactionItemDto transactionItemDto = new TransactionItemDto();
                transactionItemDto.setTransaction(transactionDto.getId());
                transactionItemDto.setProduct(productDto.getId());
                transactionItemDto.setUnitPrice(productDto.getSellingPrice());
                transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure());
                transactionItemDto.setQuantity(item.getQuantity());
                transactionService.addItem(transactionItemDto);
            }
            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/sales-order", method = RequestMethod.GET)
    public ResponseEntity<?> getSalesOrder() {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.SALES_ORDER);
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
    public ResponseEntity<?> editSalesOrder(@PathVariable String id, @RequestBody SalesOrderEditDto salesOrderEditDto) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            transactionDto.setStatus(salesOrderEditDto.getStatus());
            transactionDto.setDescription(salesOrderEditDto.getDescription());
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
    @RequestMapping(value = "/sales-order/{id}/items", method = RequestMethod.GET)
    public ResponseEntity<?> getItems(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.getItems(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
