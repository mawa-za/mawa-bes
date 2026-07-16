package za.co.mawa.bes.entity.access;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.util.Date;

@Entity
@Table(name="user_access_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccessAuditEntity {
    @Id @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid") private String id;
    @Column(name="user_id", length=255) private String userId;
    @Column(name="username", length=150) private String username;
    @Column(name="action", nullable=false, length=100) private String action;
    @Column(name="target_type", length=80) private String targetType;
    @Column(name="target_id", length=255) private String targetId;
    @Column(name="reason", length=500) private String reason;
    @Lob @Column(name="details") private String details;
    @Column(name="created_at", nullable=false) @Temporal(TemporalType.TIMESTAMP) private Date createdAt;
    @PrePersist void onCreate(){ if(createdAt == null) createdAt = new Date(); }
}
