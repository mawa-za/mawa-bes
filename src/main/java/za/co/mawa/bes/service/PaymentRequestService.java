package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.PaymentRequestDao;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestCreateDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PaymentRequestService implements PaymentRequestDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    UserService userService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;

    @Override
    public String create(PaymentRequestCreateDto paymentRequestCreateDto) throws Exception {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.PAYMENT_REQUEST);
        transactionCreateDto.setSubType(paymentRequestCreateDto.getPaymentMethod());
        transactionCreateDto.setCategory(paymentRequestCreateDto.getPaymentReason());
        transactionCreateDto.setStatus(Status.NEW);
        transactionCreateDto.setLocation(paymentRequestCreateDto.getBranch());
        TransactionDto transaction = transactionService.create(transactionCreateDto);
        if (transaction.getId() != null) {
            if (paymentRequestCreateDto.getAmount() != null) {
                TransactionAmountDto amount = new TransactionAmountDto();
                amount.setAmount(paymentRequestCreateDto.getAmount());
                amount.setTransaction(transaction.getId());
                amount.setType(PriceType.PAYMENT_AMOUNT);
                transactionService.addAmount(amount);
            }
            if (paymentRequestCreateDto.getEmployeeResponsibleId() != null && paymentRequestCreateDto.getEmployeeResponsibleId() != "") {
                TransactionPartnerDto partner = new TransactionPartnerDto();
                partner.setFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
                partner.setPartner(paymentRequestCreateDto.getEmployeeResponsibleId());
                partner.setTransaction(transaction.getId());
                transactionService.addPartner(partner);
            }
            if (paymentRequestCreateDto.getRecipientId() != null && paymentRequestCreateDto.getRecipientId() != "") {
                TransactionPartnerDto partner = new TransactionPartnerDto();
                partner.setFunction(PartnerFunction.RECIPIENT);
                partner.setPartner(paymentRequestCreateDto.getRecipientId());
                partner.setTransaction(transaction.getId());
                transactionService.addPartner(partner);
            }
            if (paymentRequestCreateDto.getBankAccount() != null) {
                TransactionAccountDto account = new TransactionAccountDto();
                account.setAccountHolder(paymentRequestCreateDto.getBankAccount().getAccountHolder());
                account.setTransaction(transaction.getId());
                account.setAccountNumber(paymentRequestCreateDto.getBankAccount().getAccountNumber());
                account.setBankName(paymentRequestCreateDto.getBankAccount().getBankName());
                account.setBranchCode(paymentRequestCreateDto.getBankAccount().getBranchCode());
                account.setAccountType(paymentRequestCreateDto.getBankAccount().getAccountType());
                transactionService.addBankAccount(account);
            }
            TransactionDateDto dateDto = new TransactionDateDto();
            dateDto.setValue(new Date());
            dateDto.setTransaction(transaction.getId());
            dateDto.setType(DateType.CREATED);
            transactionService.addDate(dateDto);
            if (paymentRequestCreateDto.getDueDate() != null) {
                dateDto = new TransactionDateDto();
                dateDto.setValue(paymentRequestCreateDto.getDueDate());
                dateDto.setTransaction(transaction.getId());
                dateDto.setType(DateType.DUE_DATE);
                transactionService.addDate(dateDto);

            }
            if (paymentRequestCreateDto.getReference() != null && paymentRequestCreateDto.getReference() != "") {
                TransactionLinkDto linkDto = new TransactionLinkDto();
                linkDto.setTransaction1(transaction.getId());
                linkDto.setTransaction2(paymentRequestCreateDto.getReference());
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
        paymentRequestDto.setCreatedBy(transactionDto.getCreatedBy());
        paymentRequestDto.setStatus(fieldOptionService.getFieldOption(Field.STATUS, transactionDto.getStatus()));
        paymentRequestDto.setPaymentMethod(fieldOptionService.getFieldOption(Field.PAYMENT_METHOD, transactionDto.getSubType()));
        paymentRequestDto.setPaymentReason(fieldOptionService.getFieldOption(Field.PAYMENT_REASON, transactionDto.getCategory()));
        paymentRequestDto.setBranch(fieldOptionService.getFieldOption(Field.BRANCH, transactionDto.getLocation()));
        for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
            if (transactionDateDto.getType().equalsIgnoreCase(DateType.DUE_DATE)) {
                paymentRequestDto.setDueDate(transactionDateDto.getValue());
            }
            if (transactionDateDto.getType().equalsIgnoreCase(DateType.CREATED)) {
                paymentRequestDto.setCreatedDate(transactionDateDto.getValue());
            }
        }
        for (TransactionPartnerDto transactionPartner : transactionService.getPartners(id)) {
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                paymentRequestDto.setEmployeeResponsible(partnerService.get(transactionPartner.getPartner()));
            }
            if (transactionPartner.getFunction().equalsIgnoreCase(PartnerFunction.RECIPIENT)) {
                paymentRequestDto.setRecipient(partnerService.get(transactionPartner.getPartner()));
            }
        }
        TransactionAccountDto account = transactionService.getBankAccount(id);
        if (account != null) {
            BankAccountDto bankAccountDto = new BankAccountDto();
            bankAccountDto.setBankName(fieldOptionService.getFieldOption(Field.BANK_NAME, account.getBankName()));
            bankAccountDto.setAccountType(fieldOptionService.getFieldOption(Field.BANK_ACCOUNT_TYPE, account.getAccountType()));
            bankAccountDto.setAccountHolder(account.getAccountHolder());
            bankAccountDto.setAccountNumber(account.getAccountNumber());
            bankAccountDto.setBranchCode(account.getBranchCode());
            paymentRequestDto.setBankAccount(bankAccountDto);
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

    @Override
    public List<PaymentRequestDto> getAll(PaymentRequestQueryDto paymentRequestQueryDto) {
        TransactionQueryDto query = new TransactionQueryDto();
        query.setType(TransactionType.PAYMENT_REQUEST);
        List<PaymentRequestDto> requests = new ArrayList<>();
        for (String id : transactionService.search(query)) {
            try {
                requests.add(get(id));
            } catch (Exception e) {

            }
        }
        return requests;

    }
}
