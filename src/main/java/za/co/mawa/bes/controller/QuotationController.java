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
import za.co.mawa.bes.service.LineItemService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.util.Date;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "/quotation")
public class QuotationController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    LineItemService lineItemService;
    @Autowired
    ProductService productService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> postQuotation(@RequestBody QuotationCreateDto quotationCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.QUOTATION);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            if (quotationCreateDto.getQuotationDate() != null){
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.QUOTATION_DATE);
                transactionDateDto.setValue(quotationCreateDto.getQuotationDate());
                transactionService.addDate(transactionDateDto);
            }

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
                transactionService.addDate(transactionDateDto);
            }

            if (quotationCreateDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(quotationCreateDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }

            for (LineItemDto lineItemDto: quotationCreateDto.getItems()) {
                lineItemService.add(transactionDto.getId(),lineItemDto);
            }
            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getQuotations() {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.QUOTATION);
            return ResponseEntity.ok(gson.toJson(transactionService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getQuotation(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editQuotation(@PathVariable String id, @RequestBody QuotationDto quotationDto) {
        try {
            //transactionService.edit(quotationDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteQuotation(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/{id}/items", method = RequestMethod.GET)
    public ResponseEntity<?>  getItems(@PathVariable String id) {
        try {
            List<LineItemDto> lineItemDtoList = lineItemService.getAll(id);
            return ResponseEntity.ok(gson.toJson(lineItemDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/items", method = RequestMethod.POST)
    public ResponseEntity<?> postItem(@PathVariable String id, @RequestBody LineItemDto lineItemDto) {
        try {
            lineItemService.add(id,lineItemDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/items", method = RequestMethod.PUT)
    public ResponseEntity<?> putItem(@PathVariable String id, @RequestBody LineItemDto lineItemDto) {
        try {
            lineItemService.edit();
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/{id}/items", method = RequestMethod.DELETE)
    public ResponseEntity<?>  deleteItem(@PathVariable String id) {
        try {
            lineItemService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
