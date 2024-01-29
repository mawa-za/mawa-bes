package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "employment")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class EmploymentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "partner_id")
    private String partnerId;
    @Column(name = "employee_number")
    private String employeeNumber;
    @Column(name = "branch")
    private String branch;
    @Column(name = "department")
    private String department;
    @Column(name = "start_date")
    @Temporal(TemporalType.DATE)
    private Date startDate;
    @Column(name = "end_date")
    @Temporal(TemporalType.DATE)
    private Date endDate;
    @Column(name = "position")
    private String position;
    @Column(name = "status")
    private String status;
    @Column(name = "type")
    private String type;


}
