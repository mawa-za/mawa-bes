package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_number_assignment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeNumberAssignmentEntity {
    @Id
    @Column(name = "employee_number", nullable = false)
    private String employeeNumber;
    @Column(name = "partner_id", nullable = false, unique = true)
    private String partnerId;
    @Column(name = "allocated_at", insertable = false, updatable = false)
    private LocalDateTime allocatedAt;
    @Column(name = "allocated_by")
    private String allocatedBy;
}
