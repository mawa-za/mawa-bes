package za.co.mawa.bes.dto;

public class OrderDateDto {
    private String transaction;
    private String type;
    private String value;

    public OrderDateDto() {
    }

    public OrderDateDto(String transaction, String type, String value) {
        this.transaction = transaction;
        this.type = type;
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
