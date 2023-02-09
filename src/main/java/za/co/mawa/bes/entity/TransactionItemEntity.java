package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.TransactionItemDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
@Entity
@Table(name = "transaction_item")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class TransactionItemEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TransactionItemPKEntity transactionItemPKEntity;
    @Column(name = "product")
    private String product;
    @Column(name = "quantity")
    private BigDecimal quantity;
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Column(name = "unit_of_measure")
    private String unitOfMeasure;
//    @Column(name = "quantity_remaining")
//    private BigDecimal quantityRemaining;

    public TransactionItemEntity(TransactionItemDto transactionItemDto) {
        this.transactionItemPKEntity = new TransactionItemPKEntity();
        this.transactionItemPKEntity.setTransaction(transactionItemDto.getTransaction());
        this.transactionItemPKEntity.setItem(transactionItemDto.getItem());
        this.product = transactionItemDto.getProduct();
        this.quantity = transactionItemDto.getQuantity();
        this.unitOfMeasure = transactionItemDto.getBaseUnitOfMeasure();
        this.unitPrice = transactionItemDto.getUnitPrice();
        this.validFrom = transactionItemDto.getValidFrom();
        this.validTo = transactionItemDto.getValidTo();
    }
}
