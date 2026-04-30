package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import com.xero.models.accounting.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.LineItemInboundDto;
import za.co.mawa.bes.dto.invoice.*;
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.link.TransactionLinkOutboundDto;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.TransactionAttribute;
import za.co.mawa.bes.utils.TransactionLinkType;
import za.co.mawa.bes.utils.TransactionType;
import za.co.mawa.bes.xero.XeroAccountingService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping(value = "v2/invoice")
public class InvoiceControllerV2 {
    @Autowired
    TransactionService transactionService;
    Gson gson = new Gson();
    @Autowired
    PricingService pricingService;
    @Autowired
    LineItemService lineItemService;
    @Autowired
    InvoiceService invoiceService;
    @Autowired
    TransactionLinkService transactionLinkService;
    @Autowired
    TransactionAttributeService transactionAttributeService;
    @Autowired
    XeroAccountingService xeroAccountingService;
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postInvoice(@RequestBody InvoiceInboundDto invoiceInboundDto) {
        try {
            return ResponseEntity.ok(gson.toJson(invoiceService.create(invoiceInboundDto)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @RequestMapping(method =  RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllUsingView(){
        try{
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.INVOICE);

            List<TransactionViewEntity> entities = transactionService.searchV2(transactionViewDto);
            List<InvoiceQueryResultDto> invoices = new ArrayList<>();
            for(TransactionViewEntity entity: entities){
                InvoiceQueryResultDto invoiceV2Dto = new InvoiceQueryResultDto();
                invoiceV2Dto.setId(entity.getTransactionId());
                invoiceV2Dto.setTransactionNumber(entity.getTransactionNumber());
                invoiceV2Dto.setCreationDate(entity.getCreationDate());
                invoiceV2Dto.setCreatedBy(entity.getCreatedBy());
                invoiceV2Dto.setStatus(entity.getTransactionStatus());
                invoiceV2Dto.setRecipient(entity.getRecipient());
                invoiceV2Dto.setCustomer(entity.getMainPartner());
                invoiceV2Dto.setEmployeeResponsible(entity.getEmployeeResponsible());
                invoiceV2Dto.setTransactionSubType(entity.getTransactionSubtype());
                invoices.add(invoiceV2Dto);
            }
            return ResponseEntity.ok(gson.toJson(invoices));
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getInvoice(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(invoiceService.get(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> editInvoice(@PathVariable String id) {
        try {
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteInvoice(@PathVariable String id) {
        try {
            transactionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/{id}/items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?>  getItems(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(lineItemService.getAll(id)));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/items", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postItem(@PathVariable String id, @RequestBody LineItemInboundDto lineItemInboundDto) {
        try {
            lineItemInboundDto.setTransaction(id);
            lineItemService.add(lineItemInboundDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/items", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> putItem(@PathVariable String id, @RequestBody LineItemInboundDto lineItemInboundDto) {
        try {
            lineItemInboundDto.setTransaction(id);
            lineItemService.edit(lineItemInboundDto);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "/{id}/items", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?>  deleteItem(@PathVariable String id) {
        try {
            lineItemService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/pdf", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?>  getPDF(@PathVariable String id) {
        try {
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(id);
            transactionAttributeDto.setAttribute(TransactionAttribute.XERO_INVOICE_ID);
            String xeroInvoiceIdStr = transactionAttributeService.get(transactionAttributeDto);
            if(xeroInvoiceIdStr != null){
                UUID xeroInvoiceId = UUID.fromString(xeroInvoiceIdStr);
                Invoice invoice = xeroAccountingService.getInvoice(xeroInvoiceId);
                return ResponseEntity.ok(gson.toJson(invoice));
            }else{
                return ResponseEntity.ok().build();
            }

        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
