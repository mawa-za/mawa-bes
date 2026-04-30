package za.co.mawa.bes.dto.invoice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class InvoiceQueryResultDto implements Serializable {
    private String id;
    private String transactionNumber;
    private String type;
    private String transactionSubType;
    private String customer;
    private String createdBy;
    private String status;
    private Date dueDate;
    private Date creationDate;
    private String employeeResponsible;
    private String recipient;
    private String salesRepresentative;

}
