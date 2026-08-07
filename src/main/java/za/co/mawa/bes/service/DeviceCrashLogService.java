package za.co.mawa.bes.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.devicecrash.DeviceCrashLogDto;
import za.co.mawa.bes.dto.v2.devicecrash.DeviceCrashLogRequest;
import za.co.mawa.bes.entity.DeviceCrashLogEntity;
import za.co.mawa.bes.repository.DeviceCrashLogRepository;

import java.time.*;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceCrashLogService {
    private final DeviceCrashLogRepository repository;
    private final ObjectMapper mapper;

    @Transactional
    public DeviceCrashLogDto submit(DeviceCrashLogRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Crash log is required");
        String logId = trimToNull(request.getLogId());
        if (logId == null) logId = UUID.randomUUID().toString();
        final String resolvedLogId = logId;

        return repository.findByLogId(resolvedLogId).map(this::dto).orElseGet(() -> {
            LocalDateTime nowUtc = LocalDateTime.now(Clock.systemUTC());
            DeviceCrashLogEntity entity = DeviceCrashLogEntity.builder()
                    .logId(resolvedLogId)
                    .deviceId(trimToNull(request.getDeviceId()))
                    .deviceSerialNumber(trimToNull(request.getDeviceSerialNumber()))
                    .userId(trimToNull(userId))
                    .source(defaultText(request.getSource(), "UNKNOWN"))
                    .errorType(trimToNull(request.getErrorType()))
                    .errorMessage(trimToNull(request.getErrorMessage()))
                    .stackTrace(trimToNull(request.getStackTrace()))
                    .details(json(request.getDetails()))
                    .appVersion(trimToNull(request.getAppVersion()))
                    .platform(trimToNull(request.getPlatform()))
                    .deviceModel(trimToNull(request.getDeviceModel()))
                    .osVersion(trimToNull(request.getOsVersion()))
                    .occurredAt(parseDeviceTime(request.getOccurredAt(), nowUtc))
                    .receivedAt(nowUtc)
                    .build();
            return dto(repository.save(entity));
        });
    }

    public Page<DeviceCrashLogDto> list(String search, int page, int size) {
        Specification<DeviceCrashLogEntity> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("logId")), term),
                    cb.like(cb.lower(root.get("deviceId")), term),
                    cb.like(cb.lower(root.get("deviceSerialNumber")), term),
                    cb.like(cb.lower(root.get("userId")), term),
                    cb.like(cb.lower(root.get("source")), term),
                    cb.like(cb.lower(root.get("errorType")), term),
                    cb.like(cb.lower(root.get("errorMessage")), term),
                    cb.like(cb.lower(root.get("deviceModel")), term)
            ));
        }
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );
        return repository.findAll(spec, pageable).map(this::dto);
    }

    public DeviceCrashLogDto get(String logId) {
        return dto(repository.findByLogId(logId)
                .orElseThrow(() -> new IllegalArgumentException("Device crash log not found: " + logId)));
    }

    private DeviceCrashLogDto dto(DeviceCrashLogEntity entity) {
        return DeviceCrashLogDto.builder()
                .logId(entity.getLogId())
                .deviceId(entity.getDeviceId())
                .deviceSerialNumber(entity.getDeviceSerialNumber())
                .userId(entity.getUserId())
                .source(entity.getSource())
                .errorType(entity.getErrorType())
                .errorMessage(entity.getErrorMessage())
                .stackTrace(entity.getStackTrace())
                .details(parse(entity.getDetails()))
                .appVersion(entity.getAppVersion())
                .platform(entity.getPlatform())
                .deviceModel(entity.getDeviceModel())
                .osVersion(entity.getOsVersion())
                .occurredAt(entity.getOccurredAt().atOffset(ZoneOffset.UTC))
                .receivedAt(entity.getReceivedAt().atOffset(ZoneOffset.UTC))
                .build();
    }

    private LocalDateTime parseDeviceTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) return fallback;
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

    private String json(Object value) {
        if (value == null) return null;
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return value.toString();
        }
    }

    private Object parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return mapper.readValue(value, Object.class);
        } catch (Exception ignored) {
            return value;
        }
    }

    private String defaultText(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
