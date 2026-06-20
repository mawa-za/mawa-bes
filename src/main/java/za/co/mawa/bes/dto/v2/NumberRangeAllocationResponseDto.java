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
public class NumberRangeAllocationResponseDto {

    private Long id;
    private String seqType;
    private String deviceId;
    private Long fromNo;
    private Long toNo;
    private Long nextLocalNo;
    private Integer allocationSize;
    private String status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
