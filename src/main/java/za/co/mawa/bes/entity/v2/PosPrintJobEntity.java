package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="pos_print_job", indexes={@Index(name="idx_pos_print_job_claim", columnList="agent_id,status,next_attempt_at,created_at")})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PosPrintJobEntity {
    @Id @Column(length=36) private String id;
    @Column(name="receipt_id", length=255) private String receiptId;
    @Column(name="source_type", nullable=false, length=50) private String sourceType;
    @Column(name="source_id", nullable=false, length=255) private String sourceId;
    @Column(name="terminal_id", nullable=false, length=36) private String terminalId;
    @Column(name="agent_id", nullable=false, length=36) private String agentId;
    @Column(name="printer_id", nullable=false, length=36) private String printerId;
    @Column(nullable=false, columnDefinition="LONGTEXT") private String content;
    @Column(name="content_type", nullable=false, length=50) private String contentType;
    @Column(nullable=false, length=30) private String status;
    @Column(nullable=false) private int priority;
    @Column(name="idempotency_key", nullable=false, unique=true, length=64) private String idempotencyKey;
    @Column(name="attempt_count", nullable=false) private int attemptCount;
    @Column(name="max_attempts", nullable=false) private int maxAttempts;
    @Column(name="claim_token", length=36) private String claimToken;
    @Column(name="claimed_by_agent_id", length=36) private String claimedByAgentId;
    @Column(name="claimed_at") private LocalDateTime claimedAt;
    @Column(name="claim_expires_at") private LocalDateTime claimExpiresAt;
    @Column(name="next_attempt_at") private LocalDateTime nextAttemptAt;
    @Column(name="spooled_at") private LocalDateTime spooledAt;
    @Column(name="failed_at") private LocalDateTime failedAt;
    @Column(name="last_error", columnDefinition="TEXT") private String lastError;
    @Column(name="created_by", length=255) private String createdBy;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID().toString(); if(status==null) status="QUEUED"; if(contentType==null) contentType="ESC_POS_TEXT"; if(maxAttempts==0) maxAttempts=5; if(createdAt==null) createdAt=LocalDateTime.now(); }
}
