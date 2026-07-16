package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pos_print_agent")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PosPrintAgentEntity {
    @Id @Column(length = 36) private String id;
    @Column(name="agent_secret_hash", nullable=false, length=64) private String agentSecretHash;
    @Column(nullable=false, length=150) private String name;
    @Column(name="machine_name", length=150) private String machineName;
    @Column(name="os_name", length=100) private String osName;
    @Column(name="os_version", length=100) private String osVersion;
    @Column(name="agent_version", length=50) private String agentVersion;
    @Column(length=150) private String location;
    @Column(nullable=false, length=30) private String status;
    @Column(name="last_ip_address", length=100) private String lastIpAddress;
    @Column(name="last_heartbeat_at") private LocalDateTime lastHeartbeatAt;
    @Column(name="enrolled_at", nullable=false) private LocalDateTime enrolledAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID().toString(); if(status==null) status="ACTIVE"; if(enrolledAt==null) enrolledAt=LocalDateTime.now(); }
}
