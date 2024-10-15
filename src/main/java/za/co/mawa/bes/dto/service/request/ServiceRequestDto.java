package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.task.TaskDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.util.ArrayList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestDto implements Serializable {
    private String id;
    private String number;
    private FieldOptionDto subType;
    private PartnerDto customer;
    private String description;
    private FieldOptionDto category;
    private FieldOptionDto priority;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private PartnerDto createdBy;
    private PartnerDto employeeResponsible;
    private List<TaskDto> tasks;
    private List<PartnerDto> assignee;
    private Date creationDate;
    private Date dueDate;
    private PartnerDto changedBy;

    private String summary;

}