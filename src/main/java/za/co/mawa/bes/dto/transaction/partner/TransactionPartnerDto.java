package za.co.mawa.bes.dto.transaction.partner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionPartnerEntity;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionPartnerDto implements Serializable {
    private String partner;
    private String transaction;
    private String function;
    private String validFrom;
    private String validTo;
    private String status;
    private String statusReason;
    private String createdBy;
    private String createdDate;
    private String changedBy;
    public TransactionPartnerDto(TransactionPartnerEntity transactionPartnerEntity) {
        this.transaction = transactionPartnerEntity.getTransactionPartnerPKEntity().getTransaction();
        this.partner = transactionPartnerEntity.getTransactionPartnerPKEntity().getPartner();
        this.function = transactionPartnerEntity.getTransactionPartnerPKEntity().getFunction();
    }
}
