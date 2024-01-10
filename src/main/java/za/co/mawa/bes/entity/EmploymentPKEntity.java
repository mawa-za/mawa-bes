package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Date;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class EmploymentPKEntity implements Serializable {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "employee_id", length = 20)
    private String employeeId;

    @Column(name = "partner")
    private String partner;




}
