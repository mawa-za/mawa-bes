package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "group_society_member")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSocietyMemberEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "group_society_id", nullable = false)
    private String groupSocietyId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "membership_id")
    private String membershipId;

    @Column(name = "employee_no")
    private String employeeNo;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at", insertable = false)
    private Date updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

}