package za.co.mawa.bes.entity.notification;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
@Entity
@Table(name = "notification_log")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class NotificationLogEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "log_id")
    private Integer id;

    @Column(name = "notification_id", length = 255)
    private String notificationId;

    @Column(name = "action", length = 45)
    private String action;
    //@Size(max = 45)
    @Column(name = "result", length = 45)
    private String result;

    @Column(name = "executed_at")
    @Temporal(TemporalType.DATE)
    private Date executedAt;

}
