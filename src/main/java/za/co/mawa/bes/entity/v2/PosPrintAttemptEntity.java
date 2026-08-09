package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="pos_print_attempt")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PosPrintAttemptEntity {
    @Id @Column(length=36) private String id;
    @Column(name="print_job_id", nullable=false, length=36) private String printJobId;
    @Column(name="agent_id", nullable=false, length=36) private String agentId;
    @Column(name="printer_id", nullable=false, length=36) private String printerId;
    @Column(name="attempt_number", nullable=false) private int attemptNumber;
    @Column(nullable=false, length=30) private String status;
    @Column(name="started_at", nullable=false) private LocalDateTime startedAt;
    @Column(name="completed_at") private LocalDateTime completedAt;
    @Column(name="error_message", columnDefinition="TEXT") private String errorMessage;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID().toString(); if(startedAt==null) startedAt=LocalDateTime.now(); }
}
