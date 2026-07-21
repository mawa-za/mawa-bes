package za.co.mawa.bes.dto.v2.payapp;

import lombok.Data;

@Data
public class ManualCashupCreateRequest {
    private String receiptBookNo;
    private String receiptFromNo;
    private String receiptToNo;
    private String userId;
    private Long amountCents;
    private String employeeResponsibleId;
    private String employeeResponsibleName;
    private String areaCode;
    private String areaName;
    private String cashupDate;
    private String notes;
}
