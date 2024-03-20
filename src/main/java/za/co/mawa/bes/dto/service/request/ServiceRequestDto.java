package za.co.mawa.bes.dto.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ServiceRequestDto implements Serializable {
    private String id;
    private String no;
    private PartnerDto customer;
    private PartnerDto employeeResponsible;
    private PartnerDto createdBy;
    private String description;
    private FieldOptionDto category;
    private FieldOptionDto priority;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;

}
