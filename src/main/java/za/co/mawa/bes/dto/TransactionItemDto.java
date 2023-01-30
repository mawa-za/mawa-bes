package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.TransactionItemEntity;

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
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private Date validFrom;
    private Date validTo;
    public TransactionItemDto(TransactionItemEntity transactionItemEntity) {
        this.transaction = transactionItemEntity.getTransactionItemPKEntity().getTransaction();
        this.item = transactionItemEntity.getTransactionItemPKEntity().getItem();
        this.product = transactionItemEntity.getProduct();
        this.unitOfMeasure = transactionItemEntity.getUnitOfMeasure();
        this.quantity = transactionItemEntity.getQuantity();
        this.unitPrice = transactionItemEntity.getUnitPrice();
        this.validFrom = transactionItemEntity.getValidFrom();
        this.validTo = transactionItemEntity.getValidTo();
    }
}
