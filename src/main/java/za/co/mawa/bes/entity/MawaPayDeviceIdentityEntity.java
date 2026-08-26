package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mawa_pay_device_identity")
@Getter
@Setter
public class MawaPayDeviceIdentityEntity {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "device_id", nullable = false, unique = true, length = 160)
    private String deviceId;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "enrolled_by")
    private String enrolledBy;
    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion;
}
