package za.co.mawa.bes.service;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import za.co.mawa.bes.dao.PaymentRequestDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.attachment.AttachmentInboundDto;
import za.co.mawa.bes.dto.attachment.AttachmentOutboundDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestCreateDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountInboundDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.dto.transaction.link.TransactionLinkInboundDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.transaction.TransactionBankAccount;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.fnb.BankPaymentService;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.dto.PaymentInformation;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PaymentRequestService implements PaymentRequestDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    TransactionAmountService transactionAmountService;
    @Autowired
    TransactionAttributeService transactionAttributeService;
    @Autowired
    UserService userService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;
    @Autowired
    BankAccountService bankAccountService;
    @Autowired
    EmailService emailService;
    @Autowired
    SettingService settingService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    BankPaymentService bankPaymentService;
    @Autowired
    AttachmentService attachmentService;
    @Autowired
    MessageProducerService messageProducerService;
    Gson gson = new Gson();

    @Override
    public PaymentRequestDto create(PaymentRequestCreateDto paymentRequestCreateDto) throws Exception {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.PAYMENT_REQUEST);
        transactionCreateDto.setSubType(paymentRequestCreateDto.getPaymentMethod());
        transactionCreateDto.setCategory(paymentRequestCreateDto.getPaymentReason());
        transactionCreateDto.setStatus(Status.NEW);
        transactionCreateDto.setLocation(paymentRequestCreateDto.getBranch());
        TransactionDto transaction = transactionService.create(transactionCreateDto);
        if (transaction.getId() != null) {
            if (paymentRequestCreateDto.getAmount() != null) {
                try {
                    TransactionAmountInboundDto transactionAmountInboundDto = new TransactionAmountInboundDto();
                    transactionAmountInboundDto.setAmount(paymentRequestCreateDto.getAmount());
                    transactionAmountInboundDto.setTransaction(transaction.getId());
                    transactionAmountInboundDto.setType(TransactionAmount.PAYMENT_AMOUNT);
                    transactionAmountService.save(transactionAmountInboundDto);
                } catch (Exception exception) {

                }
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
            return get(transaction.getId());
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

        if (transactionDto.getDescription() == null) {
            paymentRequestDto.setDescription(transactionDto.getSubDescription());
        } else {
            paymentRequestDto.setDescription(transactionDto.getDescription());
        }

        try {
            paymentRequestDto.setCreatedBy(userService.getUserByName(transactionDto.getCreatedBy()).getPartner());
        } catch (Exception e) {

        }
        paymentRequestDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
        try {
            paymentRequestDto.setStatusReason(fieldOptionService.getFieldOption(Field.PAYMENT_REQUEST_STATUS_REASON, transactionDto.getStatusReason().toUpperCase()));
        } catch (Exception e) {

        }
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

        for (TransactionLinkDto link : transactionService.getLinks(id)) {
            if (link.getType().equalsIgnoreCase(TransactionType.PAYMENT_REQUEST)) {
                paymentRequestDto.setReference(link.getTransaction2());
            }
        }

        try {
            TransactionAttributeDto transactionAttributeDto = new TransactionAttributeDto();
            transactionAttributeDto.setTransaction(id);
            transactionAttributeDto.setAttribute(TransactionAttribute.BANK_INSTRUCTION_ID);
            transactionAttributeService.get(transactionAttributeDto);
            paymentRequestDto.setInstructionId(transactionAttributeDto.getValue());
        }catch (Exception e) {

        }

        try {
            paymentRequestDto.setAmount(transactionAmountService.getByTransaction(id).stream()
                    .filter(a -> Objects.equals(a.getType().getCode(), TransactionAmount.PAYMENT_AMOUNT))
                    .toList().iterator().next().getAmount());
        } catch (Exception exception) {
        }
        return paymentRequestDto;
    }


    @Override
    public List<PaymentRequestDto> getAll(PaymentRequestQueryDto paymentRequestQueryDto) {
        TransactionQueryDto query = new TransactionQueryDto();
        query.setType(TransactionType.PAYMENT_REQUEST);
        query.setStatus(paymentRequestQueryDto.getStatus());
        List<PaymentRequestDto> requests = new ArrayList<>();
        for (String id : transactionService.search(query)) {
            try {
                requests.add(get(id));
            } catch (Exception e) {

            }
        }
        return requests;

    }

    public List<PaymentRequestQueryDto> getAllUsingView(PaymentRequestQueryDto paymentRequestQueryDto) {
        Set<PaymentRequestQueryDto> paymentRequestQueryDtos = new HashSet<>();
        try {
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.PAYMENT_REQUEST);
            transactionViewDto.setStatus(paymentRequestQueryDto.getStatus());
            List<TransactionViewEntity> entities = transactionService.searchV2(transactionViewDto);

            for (TransactionViewEntity entity : entities) {
                PaymentRequestQueryDto dto = new PaymentRequestQueryDto();
                dto.setRecipient(entity.getRecipient());
                try {
                    dto.setAmount(new BigDecimal(entity.getAmount()));
                } catch (Exception e) {
//                    throw new RuntimeException(e);
                }
                dto.setDueDate(entity.getDueDate());
                dto.setPaymentMethod(entity.getTransactionSubtype());
                dto.setDateCreated(entity.getCreationDate());
                dto.setStatus(entity.getTransactionStatus());
                dto.setTransactionNumber(entity.getTransactionNumber());
                dto.setId(entity.getTransactionId());
                dto.setReference(entity.getReference());
                dto.setBatchNumber(entity.getBatchNumber());
                //note
                dto.setPaymentReason(entity.getCategory());
                paymentRequestQueryDtos.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(paymentRequestQueryDtos);
    }

    public void approve(TransactionProcessDto transactionProcessDto) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(transactionProcessDto.getId());
            transactionEditDto.setStatus(Status.APPROVED);
            if (transactionProcessDto.getReason() != null && transactionProcessDto.getReason() != "") {
                transactionEditDto.setStatusReason(transactionProcessDto.getReason());
            }
            if (settingService.getSetting("INTEGRATION", "FNB-API").equals("1")) {
                PaymentRequestDto paymentRequestDto = get(transactionProcessDto.getId());
                BankPaymentRequest bankPaymentRequest = bankPaymentService.generateRequest(paymentRequestDto);
                MessageQueueInboundDto messageQueueInboundDto = new MessageQueueInboundDto();
                messageQueueInboundDto.setType("FNB-EFT-PAYMENT");
                messageQueueInboundDto.setPayload(gson.toJson(bankPaymentRequest));
//                messageProducerService.sendMessage(messageQueueInboundDto);
            }
            if (settingService.getSetting("EMAIL-INVOICE", "XERO").equals("1")) {
                MessageQueueInboundDto messageQueueInboundDto = new MessageQueueInboundDto();
                messageQueueInboundDto.setType("INVOICE-EMAIL");
                messageQueueInboundDto.setPayload(transactionProcessDto.getId());
//                messageProducerService.sendMessage(messageQueueInboundDto);
            }
            transactionService.edit(transactionEditDto);
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    public void sendInvoiceFile(String id) {
        AttachmentInboundDto attachmentInboundDto = new AttachmentInboundDto();
        attachmentInboundDto.setDocumentType("INVOICE");
        attachmentInboundDto.setObjectId(id);
        try {
            AttachmentOutboundDto attachmentOutboundDto = attachmentService.getDocumentByType(attachmentInboundDto);
            String email = settingService.getSetting("INVOICE-EMAIL-ADDRESS", "XERO");
            EmailDto emailDto = new EmailDto();
            emailDto.setTo(email);
            emailDto.setSubject("Invoice");
            emailDto.setTemplate("invoice-email");
            List<PropertyDto> props = new ArrayList<>();
            List<File> files = new ArrayList<>();
            File invoice =  new File();
            invoice.setContent(attachmentOutboundDto.getFile());
            invoice.setType(attachmentOutboundDto.getExtension());
            invoice.setName("Invoice");
            files.add(invoice);
//            props.add(new PropertyDto(HtmlTemplateVariableKey.USER_FIRST_NAME,partnerDto.getName2()));
//        props.add(new PropertyDto(HtmlTemplateVariableKey.USER_PASSWORD, password));
            emailDto.setFiles(files);
            emailDto.setProperties(props);
            emailService.send(emailDto);
        } catch (DoesNotExist e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void sendToBank(String id) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.SENT_TO_BANK);
            transactionService.edit(transactionEditDto);
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

    public void complete(String id) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.SENT_TO_BANK);
            transactionService.edit(transactionEditDto);
        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }

}
