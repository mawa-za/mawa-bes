package za.co.mawa.bes.dto.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@NoArgsConstructor
@Getter
@Setter
public class TaskEditDto implements Serializable {
    private String id;
    private String number;
    private String type;
    private String description;
    private String employeeResponsibleId;
    private String plannedStartDate;
    private String plannedEndDate;
    private String actualStartDate;
    private String actualEndDate;
}
