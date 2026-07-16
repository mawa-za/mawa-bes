package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="pos_printer", uniqueConstraints=@UniqueConstraint(name="uq_pos_printer_agent_queue", columnNames={"agent_id","windows_queue_name"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PosPrinterEntity {
    @Id @Column(length=36) private String id;
    @Column(name="agent_id", nullable=false, length=36) private String agentId;
    @Column(name="windows_queue_name", nullable=false, length=255) private String windowsQueueName;
    @Column(name="display_name", nullable=false, length=255) private String displayName;
    @Column(name="printer_role", nullable=false, length=30) private String printerRole;
    @Column(nullable=false, length=30) private String status;
    @Column(name="is_default", nullable=false) private boolean defaultPrinter;
    @Column(name="supports_cut", nullable=false) private boolean supportsCut;
    @Column(name="paper_width_chars") private Integer paperWidthChars;
    @Column(name="last_seen_at") private LocalDateTime lastSeenAt;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID().toString(); if(printerRole==null) printerRole="RECEIPT"; if(status==null) status="ONLINE"; if(paperWidthChars==null) paperWidthChars=42; if(createdAt==null) createdAt=LocalDateTime.now(); }
}
