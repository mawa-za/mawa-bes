package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.service.*;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.HtmlTemplateVariableKey;

import java.util.*;

@RestController
@CrossOrigin
@RequestMapping(value = "bank-file")
public class BankFileXmlController {

    Gson gson = new Gson();
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    BankFileService bankFileService;
    @Autowired
    EmailService emailService;
    @Autowired
    SettingService settingService;
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateBankFile() {
        try {
            PaymentRequestQueryDto paymentRequestQueryDto = new PaymentRequestQueryDto();
            paymentRequestQueryDto.setStatus("APPROVED");
            List<PaymentRequestDto> requests = paymentRequestService.getAll(paymentRequestQueryDto);
            List<PaymentRequestDto> filteredRequests = requests.stream()
                    .filter(a -> Objects.nonNull(a.getAmount()))
                    .toList();

            if (!filteredRequests.isEmpty()) {
                File file = bankFileService.generateBankFile(filteredRequests);
                EmailDto emailDto = new EmailDto();
                emailDto.getFiles().add(file);
                emailDto.setTo(getEmail());
                emailDto.setSubject("Payment Batch: "+ file.getName());
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

    private String getEmail() {
        Properties properties = settingService.getSettings("BANK-PAYMENT-FILE");
        try {
            return properties.get("EMAIL-RECIPIENT").toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
