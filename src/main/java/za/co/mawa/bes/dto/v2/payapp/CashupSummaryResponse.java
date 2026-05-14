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

    private LocalDate cashupDate;

    private Long totalCents;
    private Integer receiptCount;

    private String status;

    private List<CashupPaymentSummaryDto> payments;
}