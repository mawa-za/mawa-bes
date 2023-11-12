package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.PaymentRequestDao;
import za.co.mawa.bes.dto.payment.request.PaymentRequestCreateDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.utils.*;

import java.util.Date;

@Service
public class PaymentRequestService implements PaymentRequestDao {
    @Autowired
    TransactionService transactionService;

    @Override
    public String create(PaymentRequestCreateDto paymentRequest) throws Exception {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.PAYMENT_REQUEST);
        transactionCreateDto.setSubType(paymentRequest.getPaymentMethod());
        transactionCreateDto.setCategory(paymentRequest.getPaymentReason());
        transactionCreateDto.setStatus(Status.NEW);
        transactionCreateDto.setLocation(paymentRequest.getBranch());
        TransactionDto transaction = transactionService.create(transactionCreateDto);
        if (transaction.getId() != null) {
            if (paymentRequest.getAmount() != null) {
                TransactionAmountDto amount = new TransactionAmountDto();
                amount.setAmount(paymentRequest.getAmount());
                amount.setTransaction(transaction.getId());
                amount.setType(PriceType.PAYMENT_AMOUNT);
                transactionService.addAmount(amount);
            }
            if (paymentRequest.getEmployeeResponsibleId() != null && paymentRequest.getEmployeeResponsibleId() != "") {
                TransactionPartnerDto partner = new TransactionPartnerDto();
                partner.setFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
                partner.setPartner(paymentRequest.getEmployeeResponsibleId());
                partner.setTransaction(transaction.getId());
                transactionService.addPartner(partner);
            }
            if (paymentRequest.getRecipientId() != null && paymentRequest.getRecipientId() != "") {
                TransactionPartnerDto partner = new TransactionPartnerDto();
                partner.setFunction(PartnerFunction.RECIPIENT);
                partner.setPartner(paymentRequest.getRecipientId());
                partner.setTransaction(transaction.getId());
                transactionService.addPartner(partner);
            }
            if (paymentRequest.getBankAccount() != null) {
                TransactionAccountDto account = new TransactionAccountDto();
                account.setAccountHolder(paymentRequest.getBankAccount().getAccountHolder());
                account.setTransaction(transaction.getId());
                account.setAccountNumber(paymentRequest.getBankAccount().getAccountNumber());
                account.setBankName(paymentRequest.getBankAccount().getBankName());
                account.setBranchCode(paymentRequest.getBankAccount().getBranchCode());
                account.setAccountType(paymentRequest.getBankAccount().getAccountType());
                transactionService.addBankAccount(account);
            }
            TransactionDateDto dateDto = new TransactionDateDto();
            dateDto.setValue(new Date());
            dateDto.setTransaction(transaction.getId());
            dateDto.setType(DateType.CREATED);
            transactionService.addDate(dateDto);
            if (paymentRequest.getDueDate() != null) {
                dateDto = new TransactionDateDto();
                dateDto.setValue(paymentRequest.getDueDate());
                dateDto.setTransaction(transaction.getId());
                dateDto.setType(DateType.DUE_DATE);
                transactionService.addDate(dateDto);

            }
            if (paymentRequest.getReference() != null && paymentRequest.getReference() != "") {
                TransactionLinkDto linkDto = new TransactionLinkDto();
                linkDto.setTransaction1(transaction.getId());
                linkDto.setTransaction2(paymentRequest.getReference());
                linkDto.setType(TransactionType.PAYMENT_REQUEST);
                transactionService.addLink(linkDto);
            }
            return transaction.getId();
        } else {
            return null;
        }

    }

    @Override
    public PaymentRequestDto get(String id) throws DoesNotExist, Exception {
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto();
        TransactionDto transactionDto = transactionService.get(id);
        paymentRequestDto.setId(transactionDto.getId());
        paymentRequestDto.setNumber(transactionDto.getNumber());
        paymentRequestDto.setStatus(transactionDto.getStatus());
        paymentRequestDto.setCreatedBy(transactionDto.getCreatedBy());
        paymentRequestDto.setPaymentMethod(transactionDto.getSubType());
        paymentRequestDto.setPaymentReason(transactionDto.getCategory());
        paymentRequestDto.setBranch(transactionDto.getLocation());
        for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
            if (transactionDateDto.getType().equalsIgnoreCase(DateType.DUE_DATE)) {
                paymentRequestDto.setDueDate(transactionDateDto.getValue());
            }
        }
        for (TransactionPartnerDto transactionPartner : transactionService.getPartners(id)) {
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                paymentRequestDto.setEmployeeResponsibleId(transactionPartner.getPartner());
            }
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.RECIPIENT)) {
                paymentRequestDto.setRecipientId(transactionPartner.getPartner());
            }
        }
        TransactionAccountDto account = transactionService.getBankAccount(id);
        if (account != null) {
            paymentRequestDto.setBankDetails(account);
        }
        for (TransactionLinkDto link : transactionService.getLinks(id)) {
            if (link.getType().equalsIgnoreCase(TransactionType.PAYMENT_REQUEST)) {
                paymentRequestDto.setReference(link.getTransaction2());
            }
        }
        for (TransactionAmountDto amount : transactionService.getAmounts(id)) {
            if (amount.getType().equalsIgnoreCase(PriceType.PAYMENT_AMOUNT)) {
                paymentRequestDto.setAmount(amount.getAmount());
                break;
            }
        }
        return paymentRequestDto;
    }
}
