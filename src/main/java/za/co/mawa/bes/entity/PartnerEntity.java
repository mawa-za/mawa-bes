package za.co.mawa.bes.entity;


import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author tebogomohale
 */
@Entity
@Table(name = "partner")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class PartnerEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(unique = true, name = "no", length = 20)
    private String no;
    @Column(name = "birth_date")
    @Temporal(TemporalType.DATE)
    private Date birthDate;
    @Column(name = "gender", length = 20)
    private String gender;
    @Column(name = "language", length = 20)
    private String language;
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;
    @Column(name = "name1", length = 60)
    private String name1;
    @Column(name = "name2", length = 60)
    private String name2;
    @Column(name = "name3", length = 60)
    private String name3;
    @Column(name = "title", length = 20)
    private String title;
    @Column(name = "type", length = 20)
    private String type;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Column(name = "status", length = 20)
    private String status;
    @Column(name = "status_reason", length = 20)
    private String statusReason;

}
