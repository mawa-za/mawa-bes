package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashupCreateRequestDto {

    private Long cashupNo;
    private String deviceId;
    private String userId;
    private LocalDate cashupDate;
    private Long totalCents;
    private Integer receiptCount;
    private String status;
    private String notes;
    private LocalDateTime syncedAt;
}
