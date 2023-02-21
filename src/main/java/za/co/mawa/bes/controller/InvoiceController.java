package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.dto.invoice.InvoiceCreateDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.service.InvoiceService;
import za.co.mawa.bes.service.PricingService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.util.Date;

@RestController
@RequestMapping(value = "invoice")
@CrossOrigin
public class InvoiceController {
    @Autowired
    TransactionService transactionService;
    Gson gson = new Gson();
    @Autowired
    ProductService productService;
    @Autowired
    PricingService pricingService;
    @Autowired
    @Qualifier("items")
    ItemsController itemsController;
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> postInvoice(@RequestBody InvoiceCreateDto invoiceCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.INVOICE);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            creationDate.setValue(new Date());
            transactionService.addDate(creationDate);

            if (invoiceCreateDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(invoiceCreateDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (!invoiceCreateDto.getItems().isEmpty()) {
                PricingDto pricingDto = pricingService.calculate(invoiceCreateDto.getItems());
                for (LineItemDto lineItemDto : pricingDto.getItems()) {
                    itemsController.post(transactionDto.getId(),lineItemDto);

                }
            }
            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getInvoices() {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.INVOICE);
            return ResponseEntity.ok(gson.toJson(transactionService.search(transactionQueryDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getInvoice(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(transactionService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editInvoice(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteInvoice(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/{id}/items")
    public ItemsController getItemsController(@PathVariable String id) {
        return itemsController;
    }

//    @RequestMapping(value = "{id}/items", method = RequestMethod.GET)
//    public ResponseEntity<?> getItems(@PathVariable String id) {
//        try {
//            ItemsController itemsController = ItemsController.getInstance(id);
//            return ResponseEntity.ok(gson.toJson(itemsController.getAll()));
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }
//
//    @RequestMapping(value = "{id}/items", method = RequestMethod.POST)
//    public ResponseEntity<?> postItem(@PathVariable String id, @RequestBody LineItemDto lineItemDto) {
//        try {
//            ItemsController itemsController = ItemsController.getInstance(id);
//            itemsController.post(lineItemDto);
//            return ResponseEntity.ok().build();
//        } catch (Exception exception) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }

}
