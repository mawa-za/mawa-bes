package za.co.mawa.bes.dto.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class TaskDto implements Serializable {
    private String id;
    private String parentId;
    private String number;
    private String type;
    private String description;
    private String customerId;
    private String customer;
    private String employeeResponsibleId;
    private String employeeResponsible;
    private TransactionDateDto plannedStartDate;
    private TransactionDateDto plannedEndDate;
    private Date actualStartDate;
    private Date actualEndDate;
    private String status;
}

