package za.co.mawa.bes.dto;

import za.co.mawa.bes.entity.TransactionDateEntity;

import java.util.Date;

public class TransactionDateDto {
    private String transaction;
    private String type;
    private Date value;

    public TransactionDateDto() {
    }

    public TransactionDateDto(String transaction, String type, Date value) {
        this.transaction = transaction;
        this.type = type;
        this.value = value;
    }
    public TransactionDateDto(TransactionDateEntity transactionDateEntity) {
        this.transaction = transactionDateEntity.getTransactionDatePK().getTransaction();
        this.type = transactionDateEntity.getTransactionDatePK().getType();
        this.value = transactionDateEntity.getValue();
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

    public Date getValue() {
        return value;
    }

    public void setValue(Date value) {
        this.value = value;
    }
}
