package za.co.mawa.bes.controller;

import org.springframework.transaction.TransactionStatus;
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
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionEditDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryResultDto;
import za.co.mawa.bes.service.PaymentRequestService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.ClaimStatus;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;

@RestController
@CrossOrigin
@RequestMapping(value = "payment-request")
public class PaymentRequestController {
    Gson gson = new Gson();
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    TransactionService transactionService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postPaymentRequest(@RequestBody PaymentRequestCreateDto paymentRequest) {
        try {
            PaymentRequestDto payment = new PaymentRequestDto();
            String id = paymentRequestService.create(paymentRequest);
            if (id != null) {
                payment.setId(id);
            }
            return ResponseEntity.ok().body(gson.toJson(payment));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPaymentRequest(@PathVariable String id) {
        try {
            return ResponseEntity.ok().body(gson.toJson(paymentRequestService.get(id)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPaymentRequests(@RequestParam(required = false) String status) {
        try {
            PaymentRequestQueryDto paymentRequestQueryDto = new PaymentRequestQueryDto();
            paymentRequestQueryDto.setStatus(status);
            return ResponseEntity.ok().body(gson.toJson(paymentRequestService.getAll(paymentRequestQueryDto)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex);
        }
    }

    @RequestMapping(value = "{id}/submit", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submit(@PathVariable String id) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.AWAITING_APPROVAL);
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/approve", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approve(@PathVariable String id,
                                     @RequestParam(required = false) String statusReason,
                                     @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.APPROVED);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason);
            }
            if (description != null && description != null) {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/reject", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @RequestParam(required = false) String statusReason,
                                    @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.REJECTED);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason);
            }
            if (description != null && description != null) {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "{id}/cancel", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancel(@PathVariable String id,
                                    @RequestParam(required = false) String statusReason,
                                    @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.CANCELLED);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason);
            }
            if (description != null && description != null) {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    @RequestMapping(value = "{id}/complete", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> complete(@PathVariable String id,
                                    @RequestParam(required = false) String statusReason,
                                    @RequestParam(required = false) String description) {
        try {
            TransactionEditDto transactionEditDto = new TransactionEditDto();
            transactionEditDto.setId(id);
            transactionEditDto.setStatus(Status.PROCESSED);
            if (statusReason != null && statusReason != "") {
                transactionEditDto.setStatusReason(statusReason);
            }
            if (description != null && description != null) {
                transactionEditDto.setDescription(description);
            }
            transactionService.edit(transactionEditDto);
            return ResponseEntity.ok(gson.toJson(transactionEditDto));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
