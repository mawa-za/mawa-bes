package za.co.mawa.bes.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashupRangeAllocationResponseDto {

    private Long id;
    private String deviceId;
    private long fromNo;
    private long toNo;
    private long nextNo;
    private String status;
    private Instant allocatedAt;
}
