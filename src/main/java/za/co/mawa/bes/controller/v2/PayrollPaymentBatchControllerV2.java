package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchCopyRequest;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchCreateRequest;
import za.co.mawa.bes.dto.v2.PayrollPaymentBatchResponse;
import za.co.mawa.bes.dto.v2.PayrollPaymentItemRequest;
import za.co.mawa.bes.service.v2.PayrollPaymentBatchService;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("v2/payroll-payment-batch")
@RequiredArgsConstructor
public class PayrollPaymentBatchControllerV2 {

    private final PayrollPaymentBatchService payrollPaymentBatchService;

    @PostMapping
    public ResponseEntity<PayrollPaymentBatchResponse> createBatch(
            @RequestBody PayrollPaymentBatchCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        PayrollPaymentBatchResponse response = payrollPaymentBatchService.createBatch(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sourceBatchId}/copy")
    public ResponseEntity<PayrollPaymentBatchResponse> copyPreviousBatch(
            @PathVariable String sourceBatchId,
            @RequestBody PayrollPaymentBatchCopyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        PayrollPaymentBatchResponse response =
                payrollPaymentBatchService.copyPreviousBatch(sourceBatchId, request, userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<PayrollPaymentBatchResponse> getBatch(
            @PathVariable String batchId
    ) {
        PayrollPaymentBatchResponse response = payrollPaymentBatchService.getBatch(batchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PayrollPaymentBatchResponse>> getByPayPeriod(
            @RequestParam String payPeriod
    ) {
        List<PayrollPaymentBatchResponse> response =
                payrollPaymentBatchService.getByPayPeriod(payPeriod);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{batchId}/items")
    public ResponseEntity<PayrollPaymentBatchResponse> addItem(
            @PathVariable String batchId,
            @RequestBody PayrollPaymentItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        PayrollPaymentBatchResponse response =
                payrollPaymentBatchService.addItem(batchId, request, userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{batchId}/approve")
    public ResponseEntity<PayrollPaymentBatchResponse> approveBatch(
            @PathVariable String batchId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        PayrollPaymentBatchResponse response =
                payrollPaymentBatchService.approveBatch(batchId, userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{batchId}/cancel")
    public ResponseEntity<PayrollPaymentBatchResponse> cancelBatch(
            @PathVariable String batchId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        PayrollPaymentBatchResponse response =
                payrollPaymentBatchService.cancelBatch(batchId, userId);

        return ResponseEntity.ok(response);
    }
}
