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
    @Column(name = "partner", length = 20)
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

}
