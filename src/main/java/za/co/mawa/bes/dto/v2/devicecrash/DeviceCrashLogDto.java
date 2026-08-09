package za.co.mawa.bes.dto.v2.devicecrash;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class DeviceCrashLogDto {
    String logId;
    String deviceId;
    String deviceSerialNumber;
    String userId;
    String source;
    String errorType;
    String errorMessage;
    String stackTrace;
    Object details;
    String appVersion;
    String platform;
    String deviceModel;
    String osVersion;
    OffsetDateTime occurredAt;
    OffsetDateTime receivedAt;
}
