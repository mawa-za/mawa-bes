package za.co.mawa.bes.dto.transaction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.entity.transaction.TransactionEntity;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class TransactionQueryResultDto implements Serializable {
    private String id;
    private String number;
    private String type;
    private String customerId;
    private String customerName;
    private String supplierId;
    private String supplierName;
    private String subType;
    private String description;
    private String subDescription;
    private String status;
    private String statusReason;
    private String location;
    private String subStatus;
    private Date creationDate;
    private Date orderDate;
    private Date invoiceDate;
    private Date deliveryDate;
    private Date dueDate;
    private Date expiryDate;
    private String validFrom;
    private String validTo;
    private String createdBy;
    private String changedBy;

    private MembershipDto membershipHolder;

}