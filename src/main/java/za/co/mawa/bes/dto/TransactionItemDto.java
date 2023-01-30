package za.co.mawa.bes.dto;

import za.co.mawa.bes.entity.TransactionItemEntity;

import java.math.BigDecimal;

public class TransactionItemDto {
    private String transaction;
    private String product;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private String item;

    public TransactionItemDto(TransactionItemEntity transactionItemEntity) {
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }
}
