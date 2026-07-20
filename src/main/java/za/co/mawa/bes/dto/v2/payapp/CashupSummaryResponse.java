package za.co.mawa.bes.dto.v2.payapp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CashupSummaryResponse {

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

    private Long depositTotalCents;
    private Integer depositCount;
    private String approvalRequestId;

    private List<CashupPaymentSummaryDto> payments;
    private List<CashupDepositResponse> deposits;
}
