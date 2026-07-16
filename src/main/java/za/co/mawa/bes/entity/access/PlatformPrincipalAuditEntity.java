package za.co.mawa.bes.entity.access;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.util.Date;

@Entity
@Table(name="platform_principal_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlatformPrincipalAuditEntity {
    @Id @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    private String id;
    @Column(name="handoff_id", nullable=false, unique=true, length=255) private String handoffId;
    @Column(name="platform_user_id", length=255) private String platformUserId;
    @Column(name="username", length=150) private String username;
    @Column(name="display_name", length=255) private String displayName;
    @Column(name="email", length=255) private String email;
    @Column(name="tenant_id", nullable=false, length=255) private String tenantId;
    @Column(name="erp_role_id", length=45) private String erpRoleId;
    @Column(name="access_scope", length=40) private String accessScope;
    @Column(name="account_type", length=40) private String accountType;
    @Column(name="is_test_user", nullable=false) private Boolean testUser = false;
    @Column(name="external_transactions_blocked", nullable=false) private Boolean externalTransactionsBlocked = false;
    @Column(name="access_reason", length=500) private String accessReason;
    @Column(name="ticket_reference", length=150) private String ticketReference;
    @Column(name="session_id", length=255) private String sessionId;
    @Column(name="source_ip", length=100) private String sourceIp;
    @Column(name="user_agent", length=500) private String userAgent;
    @Column(name="entered_at", nullable=false) @Temporal(TemporalType.TIMESTAMP) private Date enteredAt;
    @Column(name="exited_at") @Temporal(TemporalType.TIMESTAMP) private Date exitedAt;
    @PrePersist void onCreate(){ if(enteredAt == null) enteredAt = new Date(); }
}
