package za.co.mawa.bes.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Date;
@Entity
@Table(name = "user")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class UserEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(unique = true,name = "username", length = 100)
    private String username;
    @Column(name = "partner", length = 255)
    private String partner;
    @Column(name = "cellphone", length = 20)
    private String cellphone;
    @Column(name = "email", length = 100)
    private String email;
    @Lob
    @Column(name = "password")
    private byte[] password;
    @Column(name = "password_status", length = 20)
    private String passwordStatus;
    @Column(name = "password_changed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordChangedAt;
    @Column(name = "status", length = 20)
    private String status;
    @Column(name = "status_reason", length = 45)
    private String statusReason;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Column(name = "user_type" , length = 45)
    private String userType;
    @Column(name = "account_type", length = 40, nullable = false)
    private String accountType = "STANDARD";
    @Column(name = "is_test_user", nullable = false)
    private Boolean testUser = false;
    @Column(name = "is_protected", nullable = false)
    private Boolean protectedUser = false;
    @Column(name = "is_system_managed", nullable = false)
    private Boolean systemManaged = false;
    @Column(name = "access_scope", length = 40, nullable = false)
    private String accessScope = "STANDARD";
    @Column(name = "environment_scope", length = 255)
    private String environmentScope;
    @Column(name = "external_transactions_blocked", nullable = false)
    private Boolean externalTransactionsBlocked = false;
    @Column(name = "expires_at") @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;
    @Column(name = "protected_reason", length = 255)
    private String protectedReason;
    @Column(name = "protected_at") @Temporal(TemporalType.TIMESTAMP)
    private Date protectedAt;
    @Column(name = "protected_by", length = 255)
    private String protectedBy;
    @Column(name = "disabled_at") @Temporal(TemporalType.TIMESTAMP)
    private Date disabledAt;
    @Column(name = "disabled_by", length = 255)
    private String disabledBy;
    @Column(name = "mfa_required", nullable = false)
    private Boolean mfaRequired = false;

}
