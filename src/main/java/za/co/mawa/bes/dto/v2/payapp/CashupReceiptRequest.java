package za.co.mawa.bes.dto.v2.payapp;

import lombok.Data;

@Data
public class CashupReceiptRequest {

    private String receiptId;
    private Long receiptNo;

    private Long amountCents;
    private String paymentMethod;
}
