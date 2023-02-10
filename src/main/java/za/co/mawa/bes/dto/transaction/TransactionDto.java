package za.co.mawa.bes.dto.transaction;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionEntity;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TransactionDto implements Serializable {
    private String id;
    private String number;
    private String type;
    private String subType;
    private String description;
    private String subDescription;
    private String status;
    private String statusReason;
    private String location;
    private String subStatus;
    private String validFrom;
    private String validTo;
    private String createdBy;
    private String changedBy;

    public TransactionDto(TransactionEntity transactionEntity) {
        this.id = transactionEntity.getId();
        this.number = transactionEntity.getNumber();
        this.type = transactionEntity.getType();
        this.subType = transactionEntity.getSubType();
        this.status = transactionEntity.getStatus();
    }
}
