package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class EmploymentSearchDto implements Serializable {
    private String employeeId;
    private String number;
    private String partner;
    private Date startDate;
    private Date endDate;
    private String position;
    private String branch;
    private String department;
    private String type;
    private String status;

}
