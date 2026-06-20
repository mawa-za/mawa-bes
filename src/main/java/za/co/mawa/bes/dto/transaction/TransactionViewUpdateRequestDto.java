package za.co.mawa.bes.dto.transaction;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionViewUpdateRequestDto {

    private String transactionId;
    private String transactionType;
    private String transactionSubtype;
    private String transactionNumber;
    private String identityType;
    private String identityNumber;
    private String mainPartnerId;
    private String mainPartner;
    private String employeeResponsibleId;
    private String employeeResponsible;
    private String recipientId;
    private String recipient;
    private String createdById;
    private String changedById;
    private String changedBy;
    private String product;
    private Date creationDate;
    private Date dueDate;
    private Date deathDate;
    private Date burialDate;
    private String category;
    private String priority;
    private String reference;
    private String batchNumber;
    private String transactionStatus;
    private String claimant;
    private String amount;
    private String amountCollected;
    private String amountDeposited;
    private String dateEffective;
    private String productId;
}
