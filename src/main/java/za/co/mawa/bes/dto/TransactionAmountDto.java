package za.co.mawa.bes.dto;

import za.co.mawa.bes.entity.TransactionAmountEntity;

import java.math.BigDecimal;

public class TransactionAmountDto {
    private String transaction;
    private String type;
    private BigDecimal amount;

    public TransactionAmountDto(TransactionAmountEntity transactionAmountEntity) {
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
