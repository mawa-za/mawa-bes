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
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.*;

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

    public InvoiceDto create(InvoiceInboundDto invoiceInboundDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.INVOICE);
            transactionCreateDto.setStatus(Status.DRAFT);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);
            InvoiceDto invoiceDto = new InvoiceDto();
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

            return invoiceDto;
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
}
