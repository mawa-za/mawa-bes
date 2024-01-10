package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

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
    @EmbeddedId
    protected EmploymentPKEntity employmentPK;
    @Column(name = "end_date")
    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Column(name = "start_date")
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "position")
    private String position;

    @Column(name = "status")
    private String status;

    @Column(name = "type")
    private String type;

    @Column(name = "branch")
    private String branch;

    @Column(name = "department")
    private String department;

    @Column(name = "number")
    private String number;

}
