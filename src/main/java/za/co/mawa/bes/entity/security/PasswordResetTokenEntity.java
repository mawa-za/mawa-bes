package za.co.mawa.bes.entity.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;

@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "requested_email", nullable = false, length = 255)
    private String requestedEmail;

    @Column(name = "requested_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestedAt;

    @Column(name = "expires_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;

    @Column(name = "consumed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date consumedAt;

    @Column(name = "request_ip", length = 100)
    private String requestIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) {
            requestedAt = new Date();
        }
    }
}
