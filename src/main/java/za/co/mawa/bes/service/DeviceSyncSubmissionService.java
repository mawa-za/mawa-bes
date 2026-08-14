package za.co.mawa.bes.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import za.co.mawa.bes.dto.v2.devicesync.*;
import za.co.mawa.bes.entity.DeviceSyncSubmissionEntity;
import za.co.mawa.bes.repository.DeviceSyncSubmissionRepository;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DeviceSyncSubmissionService {
    private final DeviceSyncSubmissionRepository repository;
    private final ObjectMapper mapper;

    @Value("${device-sync.internal-base-url:http://127.0.0.1:${server.port:8080}}")
    private String internalBaseUrl;

    private final RestTemplate rest = new RestTemplate();

    @Transactional
    public DeviceSyncSubmissionDto submit(DeviceSyncSubmitRequest request, String userId) {
        validate(request);
        String key = request.getIdempotencyKey().trim();
        LocalDateTime now = LocalDateTime.now();
        Optional<DeviceSyncSubmissionEntity> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            DeviceSyncSubmissionEntity entity = existing.get();
            if (!"COMPLETED".equals(entity.getStatus())) {
                // The same durable operation may be retried after the device user
                // corrected local data. Keep the idempotency identity, but refresh
                // the payload and device metadata so reprocessing never replays the
                // stale request that originally failed.
                entity.setDeviceId(trimToNull(request.getDeviceId()));
                entity.setDeviceSerialNumber(trimToNull(request.getDeviceSerialNumber()));
                entity.setSyncTime(parseDeviceTime(request.getSyncTime(), now));
                entity.setSubmittedBy(userId);
                entity.setHttpMethod(request.getMethod().toUpperCase(Locale.ROOT));
                entity.setTargetPath(request.getPath());
                entity.setRequestPayload(json(request.getPayload()));
                entity.setResponsePayload(null);
                entity.setResponseStatus(null);
                entity.setErrorMessage(null);
                entity.setStatus("RECEIVED");
                entity.setProcessedAt(null);
                entity.setUpdatedAt(now);
                return dto(repository.save(entity));
            }
            return dto(entity);
        }

        DeviceSyncSubmissionEntity entity = DeviceSyncSubmissionEntity.builder()
                .submissionId(blank(request.getSubmissionId()) ? UUID.randomUUID().toString() : request.getSubmissionId().trim())
                .idempotencyKey(key)
                .deviceId(trimToNull(request.getDeviceId()))
                .deviceSerialNumber(trimToNull(request.getDeviceSerialNumber()))
                .syncTime(parseDeviceTime(request.getSyncTime(), now))
                .submittedBy(userId)
                .httpMethod(request.getMethod().toUpperCase(Locale.ROOT))
                .targetPath(request.getPath())
                .requestPayload(json(request.getPayload()))
                .status("RECEIVED")
                .attemptCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return dto(repository.save(entity));
    }

    public DeviceSyncSubmissionDto get(String id) {
        return dto(find(id));
    }

    public Page<DeviceSyncSubmissionDto> list(String status, String search, int page, int size) {
        Specification<DeviceSyncSubmissionEntity> spec = Specification.where(null);
        if (!blank(status) && !"ALL".equalsIgnoreCase(status)) {
            if ("ATTENTION_REQUIRED".equalsIgnoreCase(status)) {
                spec = spec.and((root, query, cb) -> root.get("status").in(
                        "CORRECTION_REQUIRED",
                        "PROCESSING_FAILED"
                ));
            } else {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.toUpperCase(Locale.ROOT)));
            }
        }
        if (!blank(search)) {
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("submissionId")), term),
                    cb.like(cb.lower(root.get("idempotencyKey")), term),
                    cb.like(cb.lower(root.get("deviceId")), term),
                    cb.like(cb.lower(root.get("deviceSerialNumber")), term),
                    cb.like(cb.lower(root.get("submittedBy")), term),
                    cb.like(cb.lower(root.get("targetPath")), term),
                    cb.like(cb.lower(root.get("errorMessage")), term)
            ));
        }
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "syncTime")
        );
        return repository.findAll(spec, pageable).map(this::dto);
    }

    @Transactional
    public DeviceSyncSubmissionDto process(String id, HttpHeaders incoming) {
        DeviceSyncSubmissionEntity entity = find(id);
        if ("COMPLETED".equals(entity.getStatus())) return dto(entity);

        entity.setStatus("PROCESSING");
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            copy(incoming, headers, HttpHeaders.AUTHORIZATION);
            copy(incoming, headers, "X-TenantID");
            copy(incoming, headers, "X-Tenant-Id");
            copy(incoming, headers, "X-UserID");
            copy(incoming, headers, "X-User-Id");
            Object body = blank(entity.getRequestPayload()) ? null : mapper.readValue(entity.getRequestPayload(), Object.class);
            ResponseEntity<String> response = rest.exchange(
                    internalBaseUrl + normalize(entity.getTargetPath()),
                    HttpMethod.valueOf(entity.getHttpMethod()),
                    new HttpEntity<>(body, headers),
                    String.class
            );
            entity.setResponseStatus(response.getStatusCode().value());
            entity.setResponsePayload(response.getBody());
            entity.setStatus("COMPLETED");
            entity.setErrorMessage(null);
            entity.setProcessedAt(LocalDateTime.now());
        } catch (HttpStatusCodeException ex) {
            entity.setResponseStatus(ex.getStatusCode().value());
            entity.setResponsePayload(ex.getResponseBodyAsString());
            entity.setErrorMessage(extractMessage(ex.getResponseBodyAsString(), ex.getMessage()));
            entity.setStatus(ex.getStatusCode().is4xxClientError() ? "CORRECTION_REQUIRED" : "PROCESSING_FAILED");
        } catch (Exception ex) {
            entity.setErrorMessage(ex.getMessage());
            entity.setStatus("PROCESSING_FAILED");
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return dto(repository.save(entity));
    }

    @Transactional
    public DeviceSyncSubmissionDto correct(String id, DeviceSyncCorrectionRequest request, String userId) {
        DeviceSyncSubmissionEntity entity = find(id);
        entity.setRequestPayload(json(request.getPayload()));
        entity.setResponsePayload(null);
        entity.setResponseStatus(null);
        entity.setErrorMessage(null);
        entity.setStatus("RECEIVED");
        entity.setSubmittedBy(userId);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setProcessedAt(null);
        return dto(repository.save(entity));
    }

    private DeviceSyncSubmissionEntity find(String id) {
        return repository.findBySubmissionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Device sync submission not found: " + id));
    }

    private void validate(DeviceSyncSubmitRequest request) {
        if (request == null || blank(request.getIdempotencyKey()) || blank(request.getMethod()) || blank(request.getPath())) {
            throw new IllegalArgumentException("idempotencyKey, method and path are required");
        }
        if (!(request.getPath().startsWith("/v2/") || request.getPath().startsWith("/pay-app/"))) {
            throw new IllegalArgumentException("Only supported device transaction endpoints may be queued");
        }
        if (Set.of("/v2/device-sync/submissions").stream().anyMatch(request.getPath()::startsWith)) {
            throw new IllegalArgumentException("Device sync endpoint cannot queue itself");
        }
        if (!Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported queued method");
        }
    }

    private DeviceSyncSubmissionDto dto(DeviceSyncSubmissionEntity entity) {
        return DeviceSyncSubmissionDto.builder()
                .submissionId(entity.getSubmissionId())
                .idempotencyKey(entity.getIdempotencyKey())
                .deviceId(entity.getDeviceId())
                .deviceSerialNumber(entity.getDeviceSerialNumber())
                .syncTime(asUtc(entity.getSyncTime()))
                .submittedBy(entity.getSubmittedBy())
                .method(entity.getHttpMethod())
                .path(entity.getTargetPath())
                .requestPayload(parse(entity.getRequestPayload()))
                .responsePayload(parse(entity.getResponsePayload()))
                .responseStatus(entity.getResponseStatus())
                .status(entity.getStatus())
                .attemptCount(entity.getAttemptCount())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .processedAt(entity.getProcessedAt())
                .build();
    }

    private LocalDateTime parseDeviceTime(String value, LocalDateTime fallback) {
        if (blank(value)) return fallback;
        try {
            return OffsetDateTime.parse(value.trim()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeException ignored) {
            try {
                return LocalDateTime.parse(value.trim());
            } catch (DateTimeException ignoredAgain) {
                return fallback;
            }
        }
    }

    private OffsetDateTime asUtc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private Object parse(String value) {
        if (blank(value)) return null;
        try {
            return mapper.readValue(value, Object.class);
        } catch (Exception exception) {
            return value;
        }
    }

    private String json(Object value) {
        try {
            return value == null ? null : mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid sync payload", exception);
        }
    }

    private String extractMessage(String body, String fallback) {
        Object value = parse(body);
        if (value instanceof Map<?, ?> map && map.get("message") != null) return map.get("message").toString();
        return fallback;
    }

    private void copy(HttpHeaders from, HttpHeaders to, String key) {
        String value = from.getFirst(key);
        if (!blank(value)) to.set(key, value);
    }

    private String normalize(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private String trimToNull(String value) {
        if (blank(value)) return null;
        return value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
