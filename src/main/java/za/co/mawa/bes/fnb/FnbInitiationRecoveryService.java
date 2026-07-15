package za.co.mawa.bes.fnb;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.fnb.dto.BankPaymentResponse;
import za.co.mawa.bes.repository.v2.ApiEndpointLogRepository;

import java.util.List;

/**
 * Recovers an instruction ID from the outbound FNB activity log when FNB accepted
 * a payment but local queue completion failed before the ID was stored.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FnbInitiationRecoveryService {

    private final ApiEndpointLogRepository apiEndpointLogRepository;
    private final ObjectMapper objectMapper;

    public String recoverInstructionId(List<String> paymentRequestReferences) {
        if (paymentRequestReferences == null || paymentRequestReferences.isEmpty()) {
            return null;
        }

        String recoveredInstructionId = null;
        for (String reference : paymentRequestReferences) {
            if (reference == null || reference.isBlank()) {
                continue;
            }

            String instructionId = apiEndpointLogRepository
                    .findLatestSuccessfulFnbInitiateByPaymentReference(reference)
                    .map(this::readInstructionId)
                    .orElse(null);

            if (instructionId == null || instructionId.isBlank()) {
                return null;
            }

            if (recoveredInstructionId != null && !recoveredInstructionId.equals(instructionId)) {
                log.warn("Different FNB instruction IDs were recovered for payment requests in the same queue message");
                return null;
            }
            recoveredInstructionId = instructionId;
        }
        return recoveredInstructionId;
    }

    private String readInstructionId(ApiEndpointLogEntity endpointLog) {
        try {
            if (endpointLog.getResponseBody() == null || endpointLog.getResponseBody().isBlank()) {
                return null;
            }
            BankPaymentResponse response = objectMapper.readValue(
                    endpointLog.getResponseBody(),
                    BankPaymentResponse.class
            );
            return response.getInstructionId();
        } catch (Exception exception) {
            log.warn("Unable to recover FNB instruction ID from API activity log {}", endpointLog.getId(), exception);
            return null;
        }
    }
}
