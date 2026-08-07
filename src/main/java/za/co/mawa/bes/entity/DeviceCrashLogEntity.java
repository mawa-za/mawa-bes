package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_crash_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCrashLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_id", nullable = false, unique = true, length = 36)
    private String logId;

    @Column(name = "device_id", length = 160)
    private String deviceId;

    @Column(name = "device_serial_number", length = 160)
    private String deviceSerialNumber;

    @Column(name = "user_id", length = 160)
    private String userId;

    @Column(name = "source", nullable = false, length = 60)
    private String source;

    @Column(name = "error_type", length = 160)
    private String errorType;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Lob
    @Column(name = "stack_trace", columnDefinition = "LONGTEXT")
    private String stackTrace;

    @Lob
    @Column(name = "details", columnDefinition = "LONGTEXT")
    private String details;

    @Column(name = "app_version", length = 80)
    private String appVersion;

    @Column(name = "platform", length = 80)
    private String platform;

    @Column(name = "device_model", length = 160)
    private String deviceModel;

    @Column(name = "os_version", length = 160)
    private String osVersion;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
