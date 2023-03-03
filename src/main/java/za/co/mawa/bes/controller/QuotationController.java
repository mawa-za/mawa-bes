package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.quotation.QuotationCreateDto;
import za.co.mawa.bes.dto.quotation.QuotationDto;
import za.co.mawa.bes.dto.quotation.QuotationQueryDto;
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
public class QuotationController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    Gson gson = new Gson();

    @RequestMapping(value = "/quotation", method = RequestMethod.POST)
    public ResponseEntity<?> postQuotation(@RequestBody QuotationCreateDto quotationCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.QUOTATION);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            creationDate.setValue(new Date());
            transactionService.addDate(creationDate);

            if (quotationCreateDto.getDeliveryDate() != null){
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.DELIVERY_DATE);
                transactionDateDto.setValue(quotationCreateDto.getDeliveryDate());
                transactionService.addDate(transactionDateDto);
            }
            if (quotationCreateDto.getExpiryDate() != null){
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.EXPIRY_DATE);
                transactionDateDto.setValue(quotationCreateDto.getExpiryDate());
                transactionService.addDate(creationDate);
            }

            if (quotationCreateDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(quotationCreateDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }

            for (LineItemDto item: quotationCreateDto.getItems()) {
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

    @RequestMapping(value = "/quotation", method = RequestMethod.GET)
    public ResponseEntity<?> getQuotations() {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.QUOTATION);
            return ResponseEntity.ok(gson.toJson(transactionService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getQuotation(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editQuotation(@PathVariable String id, @RequestBody QuotationDto quotationDto) {
        try {
            transactionService.edit(quotationDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/quotation/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteQuotation(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/quotation/{id}/items", method = RequestMethod.GET)
    public ResponseEntity<?> getItems(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.getItems(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
