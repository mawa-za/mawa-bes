package za.co.mawa.bes.controller.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.v2.payment.*;
import za.co.mawa.bes.entity.v2.PaymentRequestStatusHistoryEntity;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;
import za.co.mawa.bes.fnb.dto.BankPaymentResponse;
import za.co.mawa.bes.fnb.v2.BankPaymentService;
import za.co.mawa.bes.service.v2.PaymentRequestFnbPaymentQueueService;
import za.co.mawa.bes.service.v2.PaymentRequestService;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/v2/payment-request")
public class PaymentRequestControllerV2 {

    @Autowired
    @Qualifier("bankPaymentServiceV2")
    BankPaymentService bankPaymentService;

    private final PaymentRequestService paymentRequestService;
    private final PaymentRequestFnbPaymentQueueService fnbPaymentQueueService;
    private final za.co.mawa.bes.service.v2.PaymentDisbursementAttemptService paymentAttemptService;

    public PaymentRequestControllerV2(
            @Qualifier("paymentRequestServiceV2") PaymentRequestService paymentRequestService,
            PaymentRequestFnbPaymentQueueService fnbPaymentQueueService,
            za.co.mawa.bes.service.v2.PaymentDisbursementAttemptService paymentAttemptService
    ) {
        this.paymentRequestService = paymentRequestService;
        this.fnbPaymentQueueService = fnbPaymentQueueService;
        this.paymentAttemptService = paymentAttemptService;
    }

    @PostMapping
    public ResponseEntity<PaymentRequestResponse> create(
            @RequestBody PaymentRequestCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        return ResponseEntity.ok(paymentRequestService.create(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<PaymentRequestResponse>> getAll() {
        return ResponseEntity.ok(paymentRequestService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentRequestResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(paymentRequestService.getById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentRequestResponse>> getByStatus(@PathVariable PaymentRequestStatus status) {
        return ResponseEntity.ok(paymentRequestService.getByStatus(status));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<PaymentRequestResponse>> getByType(@PathVariable PaymentRequestType type) {
        return ResponseEntity.ok(paymentRequestService.getByType(type));
    }

    @GetMapping("/payee/{partnerId}")
    public ResponseEntity<List<PaymentRequestResponse>> getByPayeePartner(@PathVariable String partnerId) {
        return ResponseEntity.ok(paymentRequestService.getByPayeePartner(partnerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentRequestResponse> update(
            @PathVariable String id,
            @RequestBody PaymentRequestUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        return ResponseEntity.ok(paymentRequestService.update(id, request, currentUser));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<PaymentRequestResponse> submit(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        return ResponseEntity.ok(paymentRequestService.submit(id, currentUser));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<PaymentRequestResponse> updateStatus(
            @PathVariable String id,
            @RequestBody PaymentRequestStatusUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        return ResponseEntity.ok(paymentRequestService.updateStatus(id, request, currentUser));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentRequestResponse> cancel(
            @PathVariable String id,
            @RequestParam(required = false) String comment,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        return ResponseEntity.ok(paymentRequestService.cancel(id, comment, currentUser));
    }

    @PostMapping("/{id}/paid")
    public ResponseEntity<PaymentRequestResponse> markPaid(
            @PathVariable String id,
            @RequestBody MarkPaymentRequestPaidRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        return ResponseEntity.ok(paymentRequestService.markPaid(id, request, currentUser));
    }

    @PostMapping("/{id}/send-to-bank")
    public ResponseEntity<PaymentRequestResponse> sendToBank(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        fnbPaymentQueueService.queueAfterApproval(id, null, currentUser);
        return ResponseEntity.ok(paymentRequestService.getById(id));
    }

    @GetMapping("/{id}/attempts")
    public ResponseEntity<List<PaymentDisbursementAttemptResponse>> getAttempts(@PathVariable String id) {
        paymentRequestService.getById(id);
        return ResponseEntity.ok(paymentAttemptService.getAttempts(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<PaymentRequestStatusHistoryEntity>> getHistory(@PathVariable String id) {
        return ResponseEntity.ok(paymentRequestService.getHistory(id));
    }

    @RequestMapping(value = "{id}/bank-report", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BankPaymentResponse> getBankReport(@PathVariable String id) {
        try {
            PaymentRequestResponse paymentRequestResponse = paymentRequestService.getById(id);
            String instructionId = paymentRequestResponse.getFnbInstructionId();
            if (instructionId == null || instructionId.isBlank()) {
                return ResponseEntity.noContent().build();
            }

            BankPaymentResponse bankPaymentResponse = bankPaymentService.getPaymentReport(instructionId);
            return ResponseEntity.ok(bankPaymentResponse);
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
