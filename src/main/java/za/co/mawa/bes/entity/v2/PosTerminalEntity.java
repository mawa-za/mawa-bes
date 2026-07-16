package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="pos_terminal", uniqueConstraints=@UniqueConstraint(name="uq_pos_terminal_key", columnNames="terminal_key"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PosTerminalEntity {
    @Id @Column(length=36) private String id;
    @Column(name="terminal_key", nullable=false, length=100) private String terminalKey;
    @Column(name="display_name", nullable=false, length=150) private String displayName;
    @Column(length=150) private String location;
    @Column(name="agent_id", length=36) private String agentId;
    @Column(name="default_receipt_printer_id", length=36) private String defaultReceiptPrinterId;
    @Column(name="default_document_printer_id", length=36) private String defaultDocumentPrinterId;
    @Builder.Default
    @Column(nullable=false) private boolean enabled = true;
    @Column(name="last_seen_at") private LocalDateTime lastSeenAt;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID().toString(); if(createdAt==null) createdAt=LocalDateTime.now(); }
}
