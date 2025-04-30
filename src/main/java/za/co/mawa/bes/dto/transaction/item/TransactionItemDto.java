package za.co.mawa.bes.dto.transaction.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionItemDto implements Serializable {
    private String transaction;
    private String item;
    private String product;
    private String baseUnitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private Date validFrom;
    private Date validTo;
    private String status;
    public TransactionItemDto(TransactionItemEntity transactionItemEntity) {
        this.transaction = transactionItemEntity.getTransactionItemPKEntity().getTransaction();
        this.item = transactionItemEntity.getTransactionItemPKEntity().getItem();
        this.product = transactionItemEntity.getProduct();
        this.baseUnitOfMeasure = transactionItemEntity.getUnitOfMeasure();
        this.quantity = transactionItemEntity.getQuantity();
        this.unitPrice = transactionItemEntity.getUnitPrice();
        this.validFrom = transactionItemEntity.getValidFrom();
        this.validTo = transactionItemEntity.getValidTo();
    }
}
