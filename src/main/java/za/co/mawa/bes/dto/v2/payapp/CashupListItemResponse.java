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
    private LocalDate cashupDate;
    private Long totalCents;
    private Integer receiptCount;
    private String status;
    private Long depositTotalCents;
    private Integer depositCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
