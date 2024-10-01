package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.claim.ClaimQueryDto;
import za.co.mawa.bes.dto.invoice.*;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.dto.transaction.edit.TransactionPartnerEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;
import za.co.mawa.bes.service.TransactionAttributeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class InvoiceService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    PricingService pricingService;
    @Autowired
    LineItemService lineItemService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    TransactionAttributeService transactionAttributeService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    UserService userService;
    @Autowired
    TransactionRepository transactionRepository;

    public InvoiceOutboundDto create(InvoiceInboundDto invoiceInboundDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.INVOICE);

            if(invoiceInboundDto.getInvoiceType() != null){
                String invoiceType = invoiceInboundDto.getInvoiceType().trim();
                if(invoiceType.equalsIgnoreCase("APPOINTMENT")){
                    transactionCreateDto.setSubType(TransactionType.APPOINTMENT);
                }
                if(invoiceType.equalsIgnoreCase("SALES-INVOICE")){
                    transactionCreateDto.setSubType(TransactionType.SALES_INVOICE);
                }
            }
            transactionCreateDto.setCreatedBy(userService.getCurrentUser());
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            if (invoiceInboundDto.getInvoiceDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.INVOICE_DATE);
                transactionDateDto.setValue(invoiceInboundDto.getInvoiceDate());
                transactionService.addDate(transactionDateDto);
            }
            if (invoiceInboundDto.getDueDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.DUE_DATE);
                transactionDateDto.setValue(invoiceInboundDto.getDueDate());
                transactionService.addDate(transactionDateDto);
            }

            if (invoiceInboundDto.getCustomerId() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(invoiceInboundDto.getCustomerId());
                transactionService.addPartner(transactionPartnerDto);
            }
            if (invoiceInboundDto.getSalesRepresentative() != null) {
                TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setFunction(PartnerFunction.SALES_REPRESENTATIVE);
                transactionPartnerDto.setPartner(invoiceInboundDto.getSalesRepresentative());
                transactionService.addPartner(transactionPartnerDto);
            }
            for (LineItemInboundDto lineItemInboundDto : invoiceInboundDto.getItems()) {
                lineItemInboundDto.setTransaction(transactionDto.getId());
                lineItemService.add(lineItemInboundDto);
            }

            TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getDiscountAmount());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(TransactionAmount.DISCOUNT_AMOUNT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getVATAmount());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(TransactionAmount.VAT_AMOUNT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getTotalIncVat());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.TOTAL_INC_VAT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getTotalExcVat());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.TOTAL_EXC_VAT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getVATPercentage());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.VAT_PERCENTAGE);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getDiscountPercentage());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.DISCOUNT_PERCENTAGE);
            transactionAmountService.save(transactionAmountInboundDto);

            if (invoiceInboundDto.getTransactionId() != null){
                try {
                    TransactionLinkDto link = new TransactionLinkDto();
                    link.setTransaction1(transactionDto.getId());
                    link.setTransaction2(invoiceInboundDto.getTransactionId());
                    link.setType(TransactionType.APPOINTMENT);
                    link.setCreateBy(userService.getCurrentUser());
                    transactionService.addLink(link);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return get(transactionDto.getId());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public InvoiceOutboundDto get(String id) {
        InvoiceOutboundDto invoiceOutboundDto = new InvoiceOutboundDto();
        try {
            TransactionDto transactionDto = transactionService.get(id);
            invoiceOutboundDto.setId(transactionDto.getId());
            invoiceOutboundDto.setNumber(transactionDto.getNumber());
            invoiceOutboundDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            invoiceOutboundDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            invoiceOutboundDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));
            invoiceOutboundDto.setInvoiceType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getSubType()));
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(transactionDto.getId());
            transactionAttributeDto.setAttribute(TransactionAttribute.PAYMENT_METHOD);
            invoiceOutboundDto.setPaymentTerms(fieldOptionService.getFieldOption(Field.PAYMENT_TERMS, transactionAttributeService.get(transactionAttributeDto)));
            try {
                for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CUSTOMER)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            invoiceOutboundDto.setCustomer((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                    if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.SALES_REPRESENTATIVE)) {
                        if (partnerService.get(transactionPartnerDto.getPartner()) != null) {
                            invoiceOutboundDto.setSalesRepresentative((partnerService.get(transactionPartnerDto.getPartner())));
                        }
                    }
                }
            } catch (Exception e) {
            }
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                if (Objects.equals(transactionDateDto.getType(), DateType.INVOICE_DATE)) {
                    invoiceOutboundDto.setInvoiceDate(transactionDateDto.getValue());
                }
                if (Objects.equals(transactionDateDto.getType(), DateType.DUE_DATE)) {
                    invoiceOutboundDto.setDueDate(transactionDateDto.getValue());
                }
            }
            invoiceOutboundDto.setItems(lineItemService.getAll(id));
            invoiceOutboundDto.setAmounts(transactionAmountService.getByTransaction(id));
            invoiceOutboundDto.setDates(transactionService.getDates(id));
            try{
                invoiceOutboundDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
            }
            catch (Exception e){
            }
            List<TransactionLinkDto> links = transactionService.getLinks(id);
            for(TransactionLinkDto link : links){
                if(link.getType().equals(TransactionType.APPOINTMENT)){
                    TransactionEntity transaction = transactionRepository.getById(link.getTransaction2());
                    invoiceOutboundDto.setTransactionId(transaction.getId());
                }
            }
            try{
                TransactionDto transactionSubType = transactionService.get(invoiceOutboundDto.getTransactionId());
                invoiceOutboundDto.setTransactionSubType(transactionSubType);
            }
            catch(Exception ex){
            }
        } catch (TransactionNotFound exception) {
        }
        return invoiceOutboundDto;
    }

    public List<InvoiceOutboundDto> search(InvoiceQueryDto invoiceQueryDto) {
        List<InvoiceOutboundDto> invoiceOutboundDtoList = new ArrayList<>();
        try {
            TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
            if (invoiceQueryDto.getStatus() != null && invoiceQueryDto.getStatus() != "") {
                transactionQueryDto.setStatus(invoiceQueryDto.getStatus());
            }
            if (invoiceQueryDto.getNumber() != null && invoiceQueryDto.getNumber() != "") {
                transactionQueryDto.setNumber(invoiceQueryDto.getNumber());
            }
            if (invoiceQueryDto.getCustomer() != null & invoiceQueryDto.getCustomer() != "") {
                transactionQueryDto.setPartnerNo(invoiceQueryDto.getCustomer());
                transactionQueryDto.setPartnerFunction(PartnerFunction.CUSTOMER);
            }
            transactionQueryDto.setType(TransactionType.INVOICE);
            for (String id : transactionService.search(transactionQueryDto)) {
                try {
                    invoiceOutboundDtoList.add(get(id));
                } catch (Exception exception) {
                }
            }
        } catch (Exception exception) {
        }
        return invoiceOutboundDtoList;
    }

    public InvoiceOutboundDto edit(String id, InvoiceInboundDto invoiceInboundDto) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            if (transactionDto == null) {
                throw new RuntimeException("Invoice not found with ID: " + id);
            }
            if (invoiceInboundDto.getInvoiceDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.INVOICE_DATE);
                transactionDateDto.setValue(invoiceInboundDto.getInvoiceDate());
                transactionService.editDate(transactionDateDto);
            }
            if (invoiceInboundDto.getDueDate() != null) {
                TransactionDateDto transactionDateDto = new TransactionDateDto();
                transactionDateDto.setTransaction(transactionDto.getId());
                transactionDateDto.setType(DateType.DUE_DATE);
                transactionDateDto.setValue(invoiceInboundDto.getDueDate());
                transactionService.editDate(transactionDateDto);
            }
            if (invoiceInboundDto.getCustomerId() != null) {
                TransactionPartnerEdit transactionPartnerDto = new TransactionPartnerEdit();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setPartnerFunction(PartnerFunction.CUSTOMER);
                transactionPartnerDto.setPartner(invoiceInboundDto.getCustomerId());
                transactionService.partnerEdit(transactionPartnerDto);
            }
            if (invoiceInboundDto.getSalesRepresentative() != null) {
                TransactionPartnerEdit transactionPartnerDto = new TransactionPartnerEdit();
                transactionPartnerDto.setTransaction(transactionDto.getId());
                transactionPartnerDto.setPartnerFunction(PartnerFunction.SALES_REPRESENTATIVE);
                transactionPartnerDto.setPartner(invoiceInboundDto.getSalesRepresentative());
                transactionService.partnerEdit(transactionPartnerDto);
            }
            lineItemService.delete(transactionDto.getId()); // remove existing items
            for (LineItemInboundDto lineItemInboundDto : invoiceInboundDto.getItems()) {
                lineItemInboundDto.setTransaction(transactionDto.getId());
                lineItemService.add(lineItemInboundDto);
            }
            TransactionAmountInboundDto transactionAmountInboundDto  = new TransactionAmountInboundDto();;
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getDiscountAmount());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(TransactionAmount.DISCOUNT_AMOUNT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getVATAmount());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(TransactionAmount.VAT_AMOUNT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getTotalIncVat());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.TOTAL_INC_VAT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getTotalExcVat());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.TOTAL_EXC_VAT);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getVATPercentage());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.VAT_PERCENTAGE);
            transactionAmountService.save(transactionAmountInboundDto);

            transactionAmountInboundDto = new TransactionAmountInboundDto();
            transactionAmountInboundDto.setAmount(invoiceInboundDto.getPricing().getDiscountPercentage());
            transactionAmountInboundDto.setTransaction(transactionDto.getId());
            transactionAmountInboundDto.setType(AmountType.DISCOUNT_PERCENTAGE);
            transactionAmountService.save(transactionAmountInboundDto);

            return get(transactionDto.getId());
        } catch (Exception exception) {
            throw new RuntimeException("Error updating invoice: " + exception.getMessage(), exception);
        }
    }

}
