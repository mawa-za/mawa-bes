package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
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
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.HtmlTemplateVariableKey;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@RestController
@CrossOrigin
@RequestMapping(value = "batch")
public class BatchController {
    Gson gson = new Gson();
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
                EmailDto emailDto = new EmailDto();
                emailDto.getFiles().add(file);
                emailDto.setTo(getEmail());
                emailDto.setSubject(file.getOwner()+ " Payment Batch: "+ file.getName());
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
            List<TransactionViewEntity> membershipEntities = transactionService.searchV2(transactionViewDto);
            String result = membershipService.handleMembershipLapse(membershipEntities);

            return ResponseEntity.ok().body(gson.toJson(result));
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
