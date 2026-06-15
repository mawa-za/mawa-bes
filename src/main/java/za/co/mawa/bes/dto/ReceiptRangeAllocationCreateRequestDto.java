package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptRangeAllocationCreateRequestDto {

    private String deviceId;
    private long fromNo;
    private long toNo;
    private long nextNo;
    private String status;
    private Instant allocatedAt;
}
