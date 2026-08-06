package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.payment.PaymentDisbursementAttemptResponse;
import za.co.mawa.bes.entity.v2.PaymentDisbursementAttemptEntity;
import za.co.mawa.bes.enums.PaymentDisbursementAttemptStatus;
import za.co.mawa.bes.fnb.dto.BankPaymentResponse;
import za.co.mawa.bes.repository.v2.PaymentDisbursementAttemptRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentDisbursementAttemptService {

    private final PaymentDisbursementAttemptRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PaymentDisbursementAttemptResponse> getAttempts(String paymentRequestId) {
        return repository.findByPaymentRequestIdOrderByAttemptNoAsc(paymentRequestId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentDisbursementAttemptEntity ensureQueued(String paymentRequestId) {
        PaymentDisbursementAttemptEntity existing = repository
                .findFirstByPaymentRequestIdOrderByAttemptNoDesc(paymentRequestId)
                .orElse(null);

        if (existing != null && existing.getStatus() != PaymentDisbursementAttemptStatus.FAILED) {
            return existing;
        }

        PaymentDisbursementAttemptEntity attempt = new PaymentDisbursementAttemptEntity();
        attempt.setPaymentRequestId(paymentRequestId);
        attempt.setAttemptNo(existing == null ? 1 : existing.getAttemptNo() + 1);
        attempt.setProvider("FNB");
        attempt.setStatus(PaymentDisbursementAttemptStatus.QUEUED);
        return repository.save(attempt);
    }

    @Transactional
    public void markSubmitted(String paymentRequestId, String instructionId) {
        PaymentDisbursementAttemptEntity attempt = ensureQueued(paymentRequestId);
        attempt.setInstructionId(instructionId);
        attempt.setStatus(PaymentDisbursementAttemptStatus.SUBMITTED);
        if (attempt.getSubmittedAt() == null) attempt.setSubmittedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    @Transactional
    public void recordBankReport(String paymentRequestId, BankPaymentResponse report) {
        if (report == null) return;
        PaymentDisbursementAttemptEntity attempt = current(paymentRequestId);
        try {
            attempt.setBankReportJson(objectMapper.writeValueAsString(report));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store the bank report", exception);
        }
        attempt.setBankReportRetrievedAt(LocalDateTime.now());
        attempt.setLastCheckedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    @Transactional(readOnly = true)
    public Optional<BankPaymentResponse> getLatestBankReport(String paymentRequestId) {
        return repository.findFirstByPaymentRequestIdAndBankReportJsonIsNotNullOrderByAttemptNoDesc(paymentRequestId)
                .filter(attempt -> !attempt.getBankReportJson().isBlank())
                .map(attempt -> {
                    try {
                        return objectMapper.readValue(attempt.getBankReportJson(), BankPaymentResponse.class);
                    } catch (JsonProcessingException exception) {
                        throw new IllegalStateException("Stored bank report could not be read", exception);
                    }
                });
    }

    @Transactional
    public void markPending(String paymentRequestId, String providerStatus) {
        PaymentDisbursementAttemptEntity attempt = current(paymentRequestId);
        attempt.setStatus(PaymentDisbursementAttemptStatus.PENDING);
        attempt.setProviderStatus(providerStatus);
        attempt.setLastCheckedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    @Transactional
    public void markSucceeded(String paymentRequestId, String providerStatus) {
        PaymentDisbursementAttemptEntity attempt = current(paymentRequestId);
        attempt.setStatus(PaymentDisbursementAttemptStatus.SUCCEEDED);
        attempt.setProviderStatus(providerStatus);
        attempt.setLastCheckedAt(LocalDateTime.now());
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setFailureCode(null);
        attempt.setFailureMessage(null);
        repository.save(attempt);
    }

    @Transactional
    public void markFailed(String paymentRequestId, String providerStatus, String failureCode, String failureMessage) {
        PaymentDisbursementAttemptEntity attempt = current(paymentRequestId);
        attempt.setStatus(PaymentDisbursementAttemptStatus.FAILED);
        attempt.setProviderStatus(providerStatus);
        attempt.setFailureCode(failureCode);
        attempt.setFailureMessage(failureMessage);
        attempt.setLastCheckedAt(LocalDateTime.now());
        attempt.setCompletedAt(LocalDateTime.now());
        repository.save(attempt);
    }

    private PaymentDisbursementAttemptEntity current(String paymentRequestId) {
        return repository.findFirstByPaymentRequestIdOrderByAttemptNoDesc(paymentRequestId)
                .orElseGet(() -> ensureQueued(paymentRequestId));
    }

    private PaymentDisbursementAttemptResponse toResponse(PaymentDisbursementAttemptEntity entity) {
        return PaymentDisbursementAttemptResponse.builder()
                .id(entity.getId())
                .paymentRequestId(entity.getPaymentRequestId())
                .attemptNo(entity.getAttemptNo())
                .provider(entity.getProvider())
                .status(entity.getStatus())
                .instructionId(entity.getInstructionId())
                .providerStatus(entity.getProviderStatus())
                .failureCode(entity.getFailureCode())
                .failureMessage(entity.getFailureMessage())
                .bankReportAvailable(entity.getBankReportJson() != null && !entity.getBankReportJson().isBlank())
                .bankReportRetrievedAt(entity.getBankReportRetrievedAt())
                .submittedAt(entity.getSubmittedAt())
                .lastCheckedAt(entity.getLastCheckedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
