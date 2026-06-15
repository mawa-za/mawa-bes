package za.co.mawa.bes.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentUpdateRequestDto {

    private String id;
    private String partnerId;
    private String employeeNumber;
    private String branch;
    private String department;
    private Date startDate;
    private Date endDate;
    private String position;
    private String status;
    private String type;
}
