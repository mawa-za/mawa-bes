package za.co.mawa.bes.dto.transaction;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.PricingDto;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.entity.transaction.TransactionEntity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class TransactionDto implements Serializable {
    private String id;
    private String no;
    private String number;
    private String type;
    private String subType;
    private String priority;
    private String category;
    private String description;
    private String subDescription;
    private String customerId;
    private String customerName;
    private String supplierId;
    private String supplierName;
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
    private PricingDto pricing;
   private  List<ClaimDto> claimDtoList;
    private MembershipDto membershipHolder;
    private ProductDto productDetails;


    public TransactionDto(TransactionEntity transactionEntity) {
        this.id = transactionEntity.getId();
        this.number = transactionEntity.getNumber();
        this.description = transactionEntity.getDescription();
        this.type = transactionEntity.getType();
        this.subType = transactionEntity.getSubType();
        this.status = transactionEntity.getStatus();
        this.creationDate = transactionEntity.getValidFrom();
    }
}
