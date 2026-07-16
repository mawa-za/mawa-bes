package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="pos_print_enrollment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PosPrintEnrollmentEntity {
    @Id @Column(length=36) private String id;
    @Column(name="code_hash", nullable=false, unique=true, length=64) private String codeHash;
    @Column(name="agent_name", nullable=false, length=150) private String agentName;
    @Column(length=150) private String location;
    @Column(name="expires_at", nullable=false) private LocalDateTime expiresAt;
    @Column(name="used_at") private LocalDateTime usedAt;
    @Column(name="created_by", length=255) private String createdBy;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID().toString(); if(createdAt==null) createdAt=LocalDateTime.now(); }
}
