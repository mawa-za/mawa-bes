package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.LayByDao;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.layby.LayByCreateDto;
import za.co.mawa.bes.dto.layby.LayByDto;
import za.co.mawa.bes.dto.layby.LayByEditDto;
import za.co.mawa.bes.dto.layby.LayByQueryDto;
import za.co.mawa.bes.dto.layby.LayByGetDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionDateEdit;
import za.co.mawa.bes.dto.transaction.edit.TransactionEdit;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.utils.*;

import java.util.Calendar;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

@Service
public class LayByService implements LayByDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    ReceiptService receiptService;
    @Autowired
    ProductService productService;
    @Autowired
    PartnerService partnerService;
    @Override
    public String create(LayByCreateDto layByCreateDto) throws Exception {
        try{
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.LAYBY);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            if(layByCreateDto.getProductId() != null){
                ProductDto productDto = productService.get(layByCreateDto.getProductId());
                TransactionItemDto transactionItemDto = new TransactionItemDto();
                transactionItemDto.setTransaction(transactionDto.getId());
                transactionItemDto.setProduct(layByCreateDto.getProductId());
                transactionItemDto.setProduct(productDto.getId());
                transactionItemDto.setUnitPrice(productDto.getSellingPrice());
                transactionItemDto.setBaseUnitOfMeasure(productDto.getBaseUnitOfMeasure());
                transactionItemDto.setQuantity(new BigDecimal("1"));
                transactionService.addItem(transactionItemDto);
            }
            if(layByCreateDto.getSalesRepresentativeId() != null){
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
                transactionPartnerDto.setPartner(layByCreateDto.getSalesRepresentativeId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if(layByCreateDto.getCustomerId() != null){
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(layByCreateDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }
            TransactionDateDto creationDate = new TransactionDateDto();
            creationDate.setTransaction(transactionDto.getId());
            creationDate.setType(DateType.CREATED);
            creationDate.setValue(new Date());
            transactionService.addDate(creationDate);
            if(layByCreateDto.getPeriod() > 0)
            {
                Date today = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(today);
                calendar.add(Calendar.MONTH, layByCreateDto.getPeriod());
                Date expiryDate = calendar.getTime();
                TransactionDateDto expriy = new TransactionDateDto();
                expriy.setTransaction(transactionDto.getId());
                expriy.setType(DateType.EXPIRY_DATE);
                expriy.setValue(expiryDate);
                transactionService.addDate(expriy);
            }
            return transactionDto.getId();
        }catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public LayByDto get(String id) throws Exception {
        try{
            LayByDto layByDetails = new LayByDto();
            TransactionDto transactionDto = transactionService.get(id);

            layByDetails.setId(transactionDto.getId());
            layByDetails.setNumber(transactionDto.getNumber());
            layByDetails.setStatus(transactionDto.getStatus());
            layByDetails.setStatusReason(transactionDto.getStatusReason());
            layByDetails.setCreatedBy(transactionDto.getCreatedBy());

            for(TransactionPartnerDto partner:transactionService.getPartners(id)){
                if(partner.getFunction().equalsIgnoreCase(PartnerFunction.CUSTOMER)){
                    PartnerDto partnerCustomer =  partnerService.getOptional(partner.getPartner());
                    if(partnerCustomer != null){
                        layByDetails.setCustomer(partnerCustomer);
                    }
                }
                if (partner.getFunction().equalsIgnoreCase(PartnerFunction.SALES_REPRESENTATIVE)){
                    PartnerDto partnerSale =  partnerService.getOptional(partner.getPartner());
                    if(partnerSale != null){
                        layByDetails.setSalesRepresentative(partnerSale);
                    }

                }
            }
            for(TransactionDateDto dates:transactionService.getDates(id)){
                if(dates.getType().equalsIgnoreCase(DateType.CREATED)){
                  layByDetails.setDateCreated(Conversion.dateToString(dates.getValue()));
                }
                if(dates.getType().equalsIgnoreCase(DateType.EXPIRY_DATE)){
                  layByDetails.setEndDate(Conversion.dateToString(dates.getValue()));
                }
            }
            ReceiptSearchDto receiptSearch = new ReceiptSearchDto();
            receiptSearch.setReceiptType(TransactionType.LAYBY);
            receiptSearch.setInvoiceNumber(layByDetails.getNumber());
            layByDetails.setReceipts(receiptService.getReceipts(receiptSearch));
            for(TransactionAmountDto amount: transactionService.getAmounts(id)){
                if(amount.getType().equalsIgnoreCase(PriceType.TOTAL_INC_VAT)){
                    layByDetails.setAmount(amount.getAmount());
                }
            }
            String productId = null;
            for(TransactionItemDto item:transactionService.getItems(id)){
                if(item.getValidTo().compareTo(new Date()) > 0){
                    productId = item.getProduct();
                }
            }
            ProductDto productDto =  productService.getOptionalById(productId);
            if(productDto != null){
                layByDetails.setProductDetails(productDto);
            }
            return layByDetails;
        }catch (Exception exception){
            throw new RuntimeException(exception);
        }

    }

    @Override
    public ArrayList<LayByGetDto> search(LayByQueryDto queryDto) throws Exception {
        try{
            ArrayList<LayByGetDto> details = new ArrayList<>();
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            transactionQueryDto.setType(TransactionType.LAYBY);
            if(queryDto.getNumber() != null){
                transactionQueryDto.setNumber(queryDto.getNumber());
            }
            if(queryDto.getStatus() != null){
                transactionQueryDto.setStatus(queryDto.getStatus());
            }
            if(queryDto.getCustomerId() != null){
                transactionQueryDto.setPartnerNo(queryDto.getCustomerId());
                transactionQueryDto.setPartnerFunction(PartnerFunction.CUSTOMER);
            }
            if(queryDto.getSalesRepresentativeId() != null){
                transactionQueryDto.setPartnerNo(queryDto.getSalesRepresentativeId());
                transactionQueryDto.setPartnerFunction(PartnerFunction.SALES_REPRESENTATIVE);
            }
            if(queryDto.getEndDate() != null){
                transactionQueryDto.setDateType(DateType.EXPIRY_DATE);
                transactionQueryDto.setValue(Conversion.stringToDate(queryDto.getEndDate()));
            }
            if(queryDto.getDateCreated() != null){
                transactionQueryDto.setDateType(DateType.CREATED);
                transactionQueryDto.setValue(Conversion.stringToDate(queryDto.getDateCreated()));
            }
            for(TransactionQueryResultDto queryResults:transactionService.search(transactionQueryDto)){
                LayByGetDto layby = new LayByGetDto();
                layby.setId(queryResults.getId());
                layby.setNumber(queryResults.getNumber());
                layby.setStatus(queryResults.getStatus());
                layby.setStatusReason(queryResults.getStatusReason());
                for(TransactionPartnerDto partnerDto:transactionService.getPartners(queryResults.getId())){
                    if(partnerDto.getFunction().equalsIgnoreCase(PartnerFunction.CUSTOMER)){
                        PartnerDto partner = partnerService.getOptional(partnerDto.getPartner());
                        if(partner != null){
                            layby.setCustomer(partner);
                        }
                        break;
                    }
                }
                for(TransactionAmountDto amount:transactionService.getAmounts(queryResults.getId())){
                    if(amount.getType().equalsIgnoreCase(PriceType.TOTAL_INC_VAT)){
                      layby.setAmount(amount.getAmount());
                      break;
                    }
                }
                for(TransactionDateDto dates:transactionService.getDates(queryResults.getId())){
                   if(dates.getType().equalsIgnoreCase(DateType.CREATED)){
                       layby.setDateCreated(Conversion.dateToString(dates.getValue()));
                   }
                    if(dates.getType().equalsIgnoreCase(DateType.EXPIRY_DATE)){
                        layby.setEndDate(Conversion.dateToString(dates.getValue()));
                    }
                }
                details.add(layby);
            }
            return details;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
    @Override
    public boolean delete(String id) throws Exception {
        try{
            transactionService.delete(id);
            return true;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean edit(String id, LayByEditDto editDto) throws Exception {
        try{
            boolean edited = false;
            TransactionEdit edit = new TransactionEdit();
            edit.setId(id);
            if(editDto.getStatus() != null && editDto.getStatus() != ""){
                edit.setStatus(editDto.getStatus());
                edited = transactionService.edit(edit);
            }
            if(editDto.getStatusReason() != null && editDto.getStatusReason() != ""){
                edit.setStatusReason(editDto.getStatusReason());
                edited = transactionService.edit(edit);
            }
            if(editDto.getEndDate() != null && editDto.getEndDate() != ""){
                TransactionDateEdit dateEdit = new TransactionDateEdit();
                dateEdit.setType(DateType.EXPIRY_DATE);
                dateEdit.setValue(Conversion.stringToDate(editDto.getEndDate()));
                dateEdit.setTransaction(id);
                edited = transactionService.dateEdit(dateEdit);
            }
            return edited;
        }catch(Exception exception){
            throw new RuntimeException(exception);
        }

    }
}
