package za.co.mawa.bes.dto.v2.payapp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CashupListItemResponse {
    private String id;
    private Long cashupNo;
    private String deviceId;
    private String userId;
    private String cashierName;
    private LocalDate cashupDate;
    private Long totalCents;
    private Integer receiptCount;
    private String status;
    private String source;
    private String receiptBookNo;
    private String receiptFromNo;
    private String receiptToNo;
    private Long manualAmountCents;
    private Long receiptTotalCents;
    private Long varianceCents;
    private String employeeResponsibleId;
    private String employeeResponsibleName;
    private String areaCode;
    private String areaName;
    private Long depositTotalCents;
    private Integer depositCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
