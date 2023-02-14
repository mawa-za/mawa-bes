package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.purchase.order.PurchaseOrderCreateDto;
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

import java.math.BigDecimal;
import java.util.Date;

@RestController
@CrossOrigin
public class PurchaseOrderController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    Gson gson = new Gson();

    @RequestMapping(value = "/purchase-order", method = RequestMethod.POST)
    public ResponseEntity<?> postPurchaseOrder(@RequestBody PurchaseOrderCreateDto purchaseOrderCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.PURCHASE_ORDER);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            creationDate.setValue(new Date());
            transactionService.addDate(creationDate);

            if (purchaseOrderCreateDto.getSupplierId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.SUPPLIER);
                transactionPartnerDto.setPartner(purchaseOrderCreateDto.getSupplierId());
                transactionService.addPartner(transactionPartnerDto);
            }

            for (LineItemDto item: purchaseOrderCreateDto.getItems()) {
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

    @RequestMapping(value = "/purchase-order", method = RequestMethod.GET)
    public ResponseEntity<?> getPurchaseOrder() {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.PURCHASE_ORDER);
            return ResponseEntity.ok(gson.toJson(transactionService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/purchase-order/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getPurchaseOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/purchase-order/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editPurchaseOrder(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/purchase-order/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deletePurchaseOrder(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/purchase-order/{id}/items", method = RequestMethod.GET)
    public ResponseEntity<?> getItems(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.getItems(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
