package za.co.mawa.bes.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dao.PaymentRequestDao;
import za.co.mawa.bes.dto.payment.request.PaymentRequestCreateDto;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryResultDto;
import za.co.mawa.bes.service.PaymentRequestService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;

@RestController
@CrossOrigin
public class PaymentRequestController {
    Gson gson = new Gson();
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    TransactionService transactionService;
    @RequestMapping(value = "/payment-request", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postPaymentRequest(@RequestBody PaymentRequestCreateDto paymentRequest) {
        try{
            PaymentRequestDto payment = new PaymentRequestDto();
            String id = paymentRequestService.create(paymentRequest);
            if(id != null)
            {
                payment.setId(id);
            }
            return ResponseEntity.ok().body(gson.toJson(payment));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value = "/payment-request/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPaymentRequest(@PathVariable String id) {
        try{
            return ResponseEntity.ok().body(gson.toJson(paymentRequestService.get(id)));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
    @RequestMapping(value = "/payment-request", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPaymentRequests() {
        try{
            TransactionQueryDto query = new TransactionQueryDto();
            ArrayList<PaymentRequestDto> requests = new ArrayList<>();
            query.setType(TransactionType.PAYMENT_REQUEST);
            for(TransactionQueryResultDto request:transactionService.search(query)) {
                requests.add(paymentRequestService.get(request.getId()));
            }
            return ResponseEntity.ok().body(gson.toJson(requests));
        }catch (Exception ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }
}
