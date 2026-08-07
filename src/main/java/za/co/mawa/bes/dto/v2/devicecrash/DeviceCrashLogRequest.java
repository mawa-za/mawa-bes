package za.co.mawa.bes.dto.v2.devicecrash;

import lombok.Data;

@Data
public class DeviceCrashLogRequest {
    private String logId;
    private String deviceId;
    private String deviceSerialNumber;
    private String source;
    private String errorType;
    private String errorMessage;
    private String stackTrace;
    private Object details;
    private String appVersion;
    private String platform;
    private String deviceModel;
    private String osVersion;
    private String occurredAt;
}
