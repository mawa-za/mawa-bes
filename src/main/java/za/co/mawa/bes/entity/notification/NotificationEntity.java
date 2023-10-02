package za.co.mawa.bes.entity.notification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "notification")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class NotificationEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "type", length = 45)
    private String type;

    @Column(name = "sender", length = 200)
    private String sender;

    @Column(name = "recipient", length = 200)
    private String recipient;

    @Column(name = "subject", length = 45)
    private String subject;

    @Lob
    @Column(name = "body")
    private String body;

    @Column(name = "created_by", length = 45)
    private String created_by;

    @Column(name = "created_at")
    @Temporal(TemporalType.DATE)
    private Date createdAt;

    @Column(name = "status", length = 45)
    private String status;

}