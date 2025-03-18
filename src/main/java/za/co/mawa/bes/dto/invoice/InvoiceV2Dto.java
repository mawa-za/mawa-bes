package za.co.mawa.bes.dto.invoice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemOutboundDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class InvoiceV2Dto implements Serializable {
    private String id;
    private String transactionNumber;
    private String type;
    private String transactionSubType;
    private String mainMember;
    private String createdBy;
    private String status;
    private Date dueDate;
    private Date creationDate;
    private String employeeResponsible;
    private String recipient;
    private String salesRepresentative;

}
