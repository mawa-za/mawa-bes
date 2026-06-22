package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;

@Entity
@Table(name = "group_society_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSocietyContactEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "group_society_id", nullable = false)
    private String groupSocietyId;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    private String role;

    @Column(name = "mobile_no")
    private String mobileNo;

    private String email;

    @Column(name = "primary_contact", nullable = false)
    private Boolean primaryContact = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at", insertable = false)
    private Date updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

}