package za.co.mawa.bes.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;

import java.io.Serializable;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransactionLinkDto implements Serializable {

    private  String transaction1;

    private String transaction2;

    private Date creationDate;
    private String createBy;
    private String type;

    public TransactionLinkDto(TransactionLinkEntity transactionLinkEntity) {
        this.transaction1 = transactionLinkEntity.getTransactionLinkPKEntity().getTransaction1();
        this.transaction2 = transactionLinkEntity.getTransactionLinkPKEntity().getTransaction2();
        this.type = transactionLinkEntity.getTransactionLinkPKEntity().getType();
        this.createBy = transactionLinkEntity.getCreated_by();
        this.creationDate = transactionLinkEntity.getCreation_date();

    }


}
