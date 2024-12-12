package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import za.co.mawa.bes.dto.BankFileXmlCreateDto;
import za.co.mawa.bes.dto.BankFileXmlDto;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.service.BankFileXmlService;
import za.co.mawa.bes.service.PaymentRequestService;

import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin
@RequestMapping(value = "generataBankFileXml")
public class BankFileXmlController {

    Gson gson = new Gson();
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    BankFileXmlService bankFileXmlService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> generateBankFile(@RequestBody BankFileXmlCreateDto bankFileCreateXmlDto) {
        try {
            PaymentRequestQueryDto paymentRequestQueryDto = new PaymentRequestQueryDto();
            paymentRequestQueryDto.setStatus("APPROVED");
            List<PaymentRequestDto> requests = paymentRequestService.getAll(paymentRequestQueryDto);
            List<PaymentRequestDto> filteredRequests = requests.stream()
                    .filter(a -> Objects.nonNull(a.getAmount()))
                    .toList();

            if (!filteredRequests.isEmpty()) {
                bankFileXmlService.generateBankFile(filteredRequests);
            }
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
        }
    }
}
