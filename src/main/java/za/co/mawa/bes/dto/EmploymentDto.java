package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class EmploymentDto implements Serializable {
    private String id;
    private String employeeNumber;
    private PartnerDto employee;
    private FieldOptionDto type;
    private String startDate;
    private String endDate;
    private String position;
    private String status;
    private FieldOptionDto branch;
    private FieldOptionDto department;
}
