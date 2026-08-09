package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_sync_submission", uniqueConstraints = @UniqueConstraint(name="uq_device_sync_submission_key", columnNames={"idempotency_key"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeviceSyncSubmissionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="submission_id", nullable=false, unique=true, length=36) private String submissionId;
    @Column(name="idempotency_key", nullable=false, length=160) private String idempotencyKey;
    @Column(name="device_id", length=160) private String deviceId;
    @Column(name="sync_time", nullable=false) private LocalDateTime syncTime;
    @Column(name="device_serial_number", length=160) private String deviceSerialNumber;
    @Column(name="submitted_by", length=160) private String submittedBy;
    @Column(name="http_method", nullable=false, length=10) private String httpMethod;
    @Column(name="target_path", nullable=false, length=500) private String targetPath;
    @Lob @Column(name="request_payload", columnDefinition="LONGTEXT") private String requestPayload;
    @Lob @Column(name="response_payload", columnDefinition="LONGTEXT") private String responsePayload;
    @Column(name="response_status") private Integer responseStatus;
    @Column(name="status", nullable=false, length=40) private String status;
    @Column(name="attempt_count", nullable=false) private int attemptCount;
    @Lob @Column(name="error_message", columnDefinition="TEXT") private String errorMessage;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    @Column(name="processed_at") private LocalDateTime processedAt;
    @Version private Long version;
}
