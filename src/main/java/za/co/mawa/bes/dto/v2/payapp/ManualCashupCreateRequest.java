package za.co.mawa.bes.dto.v2.payapp;

import lombok.Data;

@Data
public class ManualCashupCreateRequest {
    private String receiptBookNo;
    private String receiptFromNo;
    private String receiptToNo;
    private String userId;
    private String cashupDate;
    private String notes;
}
