package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.v2.devicesync.DeviceSyncSubmitRequest;
import za.co.mawa.bes.entity.DeviceSyncSubmissionEntity;
import za.co.mawa.bes.repository.DeviceSyncSubmissionRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSyncSubmissionServiceTest {

    @Mock
    private DeviceSyncSubmissionRepository repository;

    @Test
    void retryOfFailedSubmissionRefreshesCorrectedPayloadInsteadOfReplayingStaleRequest() {
        ObjectMapper mapper = new ObjectMapper();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(repository, mapper);

        DeviceSyncSubmissionEntity existing = DeviceSyncSubmissionEntity.builder()
                .id(7L)
                .submissionId("submission-1")
                .idempotencyKey("partner:device-1:42")
                .deviceId("device-1")
                .syncTime(LocalDateTime.now().minusMinutes(5))
                .submittedBy("user-old")
                .httpMethod("POST")
                .targetPath("/v2/partner")
                .requestPayload("{\"name1\":\"OLD\"}")
                .responsePayload("{\"message\":\"invalid\"}")
                .responseStatus(400)
                .status("CORRECTION_REQUIRED")
                .attemptCount(1)
                .errorMessage("invalid")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .updatedAt(LocalDateTime.now().minusMinutes(4))
                .processedAt(LocalDateTime.now().minusMinutes(4))
                .build();

        DeviceSyncSubmitRequest request = new DeviceSyncSubmitRequest();
        request.setIdempotencyKey("partner:device-1:42");
        request.setDeviceId("device-1");
        request.setDeviceSerialNumber("SERIAL-2");
        request.setMethod("POST");
        request.setPath("/v2/partner");
        request.setPayload(Map.of("name1", "CORRECTED"));

        when(repository.findByIdempotencyKey("partner:device-1:42"))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(DeviceSyncSubmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.submit(request, "user-new");

        assertThat(result.getStatus()).isEqualTo("RECEIVED");
        assertThat(existing.getRequestPayload()).contains("CORRECTED");
        assertThat(existing.getRequestPayload()).doesNotContain("OLD");
        assertThat(existing.getResponsePayload()).isNull();
        assertThat(existing.getResponseStatus()).isNull();
        assertThat(existing.getErrorMessage()).isNull();
        assertThat(existing.getProcessedAt()).isNull();
        assertThat(existing.getSubmittedBy()).isEqualTo("user-new");
        assertThat(existing.getDeviceSerialNumber()).isEqualTo("SERIAL-2");
    }

    @Test
    void completedSubmissionRemainsIdempotentAndIsNotOverwritten() {
        ObjectMapper mapper = new ObjectMapper();
        DeviceSyncSubmissionService service = new DeviceSyncSubmissionService(repository, mapper);

        DeviceSyncSubmissionEntity existing = DeviceSyncSubmissionEntity.builder()
                .id(8L)
                .submissionId("submission-2")
                .idempotencyKey("partner:device-1:43")
                .deviceId("device-1")
                .syncTime(LocalDateTime.now())
                .submittedBy("user-old")
                .httpMethod("POST")
                .targetPath("/v2/partner")
                .requestPayload("{\"name1\":\"ACCEPTED\"}")
                .responseStatus(200)
                .status("COMPLETED")
                .attemptCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DeviceSyncSubmitRequest request = new DeviceSyncSubmitRequest();
        request.setIdempotencyKey("partner:device-1:43");
        request.setDeviceId("device-1");
        request.setMethod("POST");
        request.setPath("/v2/partner");
        request.setPayload(Map.of("name1", "SHOULD-NOT-REPLACE"));

        when(repository.findByIdempotencyKey("partner:device-1:43"))
                .thenReturn(Optional.of(existing));

        var result = service.submit(request, "user-new");

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(existing.getRequestPayload()).contains("ACCEPTED");
        assertThat(existing.getRequestPayload()).doesNotContain("SHOULD-NOT-REPLACE");
    }
}
