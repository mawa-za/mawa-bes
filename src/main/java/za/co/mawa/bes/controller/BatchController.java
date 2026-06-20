package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.File;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.fnb.BankPaymentService;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.dto.PaymentInformation;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.service.BankFileService;
import za.co.mawa.bes.service.EmailService;
import za.co.mawa.bes.service.PaymentRequestService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.utils.HtmlTemplateVariableKey;

import java.time.LocalDateTime;
import java.util.*;

import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;
import za.co.mawa.bes.dto.MessageQueueCreateRequestDto;
import za.co.mawa.bes.dto.MessageQueueResponseDto;
import za.co.mawa.bes.dto.MessageQueueUpdateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionViewCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionViewResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionViewUpdateRequestDto;
import za.co.mawa.bes.mapper.MessageQueueMapper;
import za.co.mawa.bes.mapper.transaction.TransactionViewMapper;


@RestController
@CrossOrigin
@RequestMapping(value = "batch")
public class BatchController {

    private final MessageQueueMapper messageQueueMapper;
    private final TransactionViewMapper transactionViewMapper;
    Gson gson = new Gson();
    private static final Logger log = LoggerFactory.getLogger(BatchController.class);
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    BankFileService bankFileService;
    @Autowired
    EmailService emailService;
    @Autowired
    SettingService settingService;
    @Autowired
    MembershipService membershipService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    MessageQueueRepository messageQueueRepository;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    BankPaymentService bankPaymentService;

    @RequestMapping(value = "bank-file", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateBankFile() {
        try {
            List<String> ids = new ArrayList<>();
            PaymentRequestQueryDto paymentRequestQueryDto = new PaymentRequestQueryDto();
            paymentRequestQueryDto.setStatus("APPROVED");
            List<PaymentRequestQueryDto> paymentRequestQueryDtoList = paymentRequestService.getAllUsingView(paymentRequestQueryDto);
            for(PaymentRequestQueryDto paymentRequest:paymentRequestQueryDtoList){
                ids.add(paymentRequest.getId());
            }
            if (!ids.isEmpty()) {
                File file = bankFileService.generateBankFile(ids);
                if (!file.equals(null)) {
                    EmailDto emailDto = new EmailDto();
                    emailDto.getFiles().add(file);
                    emailDto.setTo(getEmail());
                    emailDto.setSubject(file.getOwner() + " Payment Batch: " + file.getName());
                    emailDto.setTemplate("payment-file-generated");
                    List<PropertyDto> props = new ArrayList<>();
                    props.add(new PropertyDto(HtmlTemplateVariableKey.IDENTIFIER, file.getName()));
                    props.add(new PropertyDto(HtmlTemplateVariableKey.PAYER, file.getOwner()));
                    emailDto.setProperties(props);
                    try {
                        emailService.send(emailDto);
                    } catch (Exception ex) {

                    }
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }


    @RequestMapping(value = "membership-lapse", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processMembershipLapse() {
        try {
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.MEMBERSHIP);
            List<TransactionViewResponseDto> membershipEntities = transactionService.searchV2(transactionViewDto);
            String result = membershipService.handleMembershipLapse(membershipEntities);

            return ResponseEntity.ok().body(gson.toJson(result));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    @RequestMapping(value = "bill-memberships", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> billAllMemberships(){
        try{
            TransactionViewDto transactionViewDto = new TransactionViewDto();
            transactionViewDto.setType(TransactionType.MEMBERSHIP);
            List<TransactionViewResponseDto> membershipEntities = transactionService.searchV2(transactionViewDto);

            Set<TransactionViewEntity> uniqueMemberships = new HashSet<>(membershipEntities);

            List<String> invoices = new ArrayList<>();
            for(TransactionViewEntity entity: uniqueMemberships){
                if(entity.getTransactionStatus().equalsIgnoreCase(String.valueOf(Status.ACTIVE)) || entity.getTransactionStatus().equalsIgnoreCase(String.valueOf(Status.NEW)) || entity.getTransactionStatus().equalsIgnoreCase(String.valueOf(Status.WAITING_PERIOD))){
                    if(entity.getTransactionId() != null){
                        invoices.add(membershipService.handleBilling(entity.getTransactionId()));
                    }
                }
            }
            return ResponseEntity.ok().body(gson.toJson(invoices));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e);
        }
    }


    @RequestMapping(value = "process-message-queue", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processMessageQueue() {
        try {
            List<MessageQueueResponseDto> messageQueueEntities = messageQueueRepository
                    .findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(LocalDateTime.now());

            for (MessageQueueEntity msg : messageQueueEntities) {
                try {
                    bankPaymentService.sendPaymentRequest(msg.getPayload());
                    msg.setProcessed(true);
                    BankPaymentRequest bankPaymentRequest = gson.fromJson(String.valueOf(msg.getPayload()), BankPaymentRequest.class);
                    for (PaymentInformation paymentInformation : bankPaymentRequest.getPaymentInformation()) {
                        paymentRequestService.sendToBank(paymentInformation.getPaymentInformationId());
                    }
                } catch (Exception e) {
                    msg.setRetryCount(msg.getRetryCount() + 1);
                    if (msg.getRetryCount() > 3) {
                        msg.setProcessed(true); // Optionally move to DeadLetterQueue
                    } else {
                        msg.setNextAttemptAt(LocalDateTime.now().plusSeconds(10));
                    }
                }
                messageQueueRepository.save(msg);
            }

            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }

    private String getEmail() {
        Properties properties = settingService.getSettings("BANK-PAYMENT-FILE");
        try {
            return properties.get("EMAIL-RECIPIENT").toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
