package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class EmploymentCreateDto implements Serializable {
    private String type;
    private String startDate;
    private String endDate;
    private String position;
    private String branch;
    private String department;
    private String partnerId;
    private String employeeNumber;
}
