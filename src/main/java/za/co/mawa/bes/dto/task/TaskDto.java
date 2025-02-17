package za.co.mawa.bes.dto.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.attribute.TransactionAttributeDto;

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
    private PartnerDto customer;
    private String employeeResponsibleId;
    private PartnerDto employeeResponsible;
    private String plannedStartDate;
    private String plannedEndDate;
    private Date actualStartDate;
    private Date actualEndDate;
    private String status;
    private String startDate;
    private String duration;
}

