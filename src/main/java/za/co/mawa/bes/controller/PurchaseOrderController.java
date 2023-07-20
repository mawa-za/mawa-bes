package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemDto;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.purchase.order.PurchaseOrderCreateDto;
import za.co.mawa.bes.dto.purchase.order.PurchaseOrderDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "/purchase-order")
public class PurchaseOrderController {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ProductService productService;
    @Autowired
    LineItemService lineItemService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    ReceiptService receiptService;
    Gson gson = new Gson();

    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postPurchaseOrder(@RequestBody PurchaseOrderCreateDto purchaseOrderCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.PURCHASE_ORDER);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

            if (purchaseOrderCreateDto.getOrderDate() != null){
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.ORDER_DATE);
                transactionDateDto.setValue(purchaseOrderCreateDto.getOrderDate());
                transactionService.addDate(transactionDateDto);
            }
            if (purchaseOrderCreateDto.getExpectedDate() != null){
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.EXPECTED_DATE);
                transactionDateDto.setValue(purchaseOrderCreateDto.getExpectedDate());
                transactionService.addDate(transactionDateDto);
            }
            if (purchaseOrderCreateDto.getSupplierId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.SUPPLIER);
                transactionPartnerDto.setPartner(purchaseOrderCreateDto.getSupplierId());
                transactionService.addPartner(transactionPartnerDto);
            }

            for (LineItemDto lineItemDto : purchaseOrderCreateDto.getItems()) {
                lineItemService.add(transactionDto.getId(), lineItemDto);
            }
            return ResponseEntity.ok(gson.toJson(transactionDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPurchaseOrder() {
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.PURCHASE_ORDER);
            ArrayList<PurchaseOrderDto> purchaseOrder = new ArrayList<>();
            for(TransactionQueryResultDto POs:transactionService.search(transactionQueryDto)){
               purchaseOrder.add(getPOOverview(POs.getId()));
            }
            return ResponseEntity.ok(gson.toJson(purchaseOrder));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPurchaseOrder(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(getPO(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editPurchaseOrder(@PathVariable String id, @RequestBody TransactionDto transactionDto) {
        try {
            //transactionService.edit(transactionDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deletePurchaseOrder(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }


    @RequestMapping(value = "/{id}/items", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getItems(@PathVariable String id) {
        try {
            List<LineItemDto> lineItemDtoList = lineItemService.getAll(id);
            return ResponseEntity.ok(gson.toJson(lineItemDtoList));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/items", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postItem(@PathVariable String id, @RequestBody LineItemDto lineItemDto) {
        try {
            lineItemService.add(id, lineItemDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "{id}/items", method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> putItem(@PathVariable String id, @RequestBody LineItemDto lineItemDto) {
        try {
            lineItemService.add(id, lineItemDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "/{id}/items", method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteItem(@PathVariable String id,@RequestParam(required = false) String itemId) {
        try {
            String idItem = itemId == null ? "" :itemId;
            lineItemService.deleteItem(id,idItem);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    private PurchaseOrderDto getPO(String id) throws TransactionNotFound {
        try{
            PurchaseOrderDto PO = new PurchaseOrderDto();
            TransactionDto transactionDto = transactionService.get(id);
            if(transactionDto.getType().equalsIgnoreCase(TransactionType.PURCHASE_ORDER)){
                BigDecimal amount = BigDecimal.ZERO;
                PO.setId(transactionDto.getId());
                PO.setNumber(transactionDto.getNumber());
                PO.setStatus(transactionDto.getStatus());
                PO.setCreatedBy(transactionDto.getCreatedBy());
                PO.setStatusReason(transactionDto.getStatusReason());
                for(LineItemDto items:lineItemService.getAll(id)){
                    amount = amount.add(items.getLineTotal());
                }
                PO.setAmount(amount);
                for(TransactionPartnerDto partners:transactionService.getPartners(id)){
                    if(partners.getFunction().equalsIgnoreCase(PartnerFunction.SUPPLIER)){
                        PartnerDto partner = partnerService.getOptional(partners.getPartner());
                        if(partner != null){
                            PO.setSupplierDetails(partner);
                        }
                        break;
                    }
                }
                for(TransactionDateDto dates:transactionService.getDates(id)){
                    if(dates.getType().equalsIgnoreCase(DateType.ORDER_DATE)){
                        PO.setOrderDate(Conversion.dateToString(dates.getValue()));
                    }
                    if(dates.getType().equalsIgnoreCase(DateType.EXPECTED_DATE)){
                        PO.setExpectedDate(Conversion.dateToString(dates.getValue()));
                    }
                }
                ReceiptSearchDto receiptSearch = new ReceiptSearchDto();
                receiptSearch.setReceiptType(TransactionType.PURCHASE_ORDER);
                receiptSearch.setInvoiceNumber(transactionDto.getNumber());
                PO.setReceipts(receiptService.getReceipts(receiptSearch));
            }
            return PO;
        }catch (Exception exception){
         throw new RuntimeException(exception);
        }

    }
    private PurchaseOrderDto getPOOverview(String id) throws TransactionNotFound {
        try{
            PurchaseOrderDto PO = new PurchaseOrderDto();
            TransactionDto transactionDto = transactionService.get(id);
            if(transactionDto.getType().equalsIgnoreCase(TransactionType.PURCHASE_ORDER)){
                BigDecimal amount = BigDecimal.ZERO;
                PO.setId(transactionDto.getId());
                PO.setNumber(transactionDto.getNumber());
                PO.setStatus(transactionDto.getStatus());
                PO.setCreatedBy(transactionDto.getCreatedBy());
                PO.setStatusReason(transactionDto.getStatusReason());
                for(LineItemDto items:lineItemService.getAll(id)){
                    amount = amount.add(items.getLineTotal());
                }
                PO.setAmount(amount);
                for(TransactionPartnerDto partners:transactionService.getPartners(id)){
                    if(partners.getFunction().equalsIgnoreCase(PartnerFunction.SUPPLIER)){
                        PartnerDto partner = partnerService.getOptional(partners.getPartner());
                        if(partner != null){
                            PO.setSupplierDetails(partner);
                        }
                        break;
                    }
                }
                for(TransactionDateDto dates:transactionService.getDates(id)){
                    if(dates.getType().equalsIgnoreCase(DateType.ORDER_DATE)){
                        PO.setOrderDate(Conversion.dateToString(dates.getValue()));
                    }
                    if(dates.getType().equalsIgnoreCase(DateType.EXPECTED_DATE)){
                        PO.setExpectedDate(Conversion.dateToString(dates.getValue()));
                    }
                }
            }
            return PO;
        }catch (Exception exception){
            throw new RuntimeException(exception);
        }

    }
}
