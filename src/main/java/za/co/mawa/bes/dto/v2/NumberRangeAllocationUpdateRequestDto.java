package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumberRangeAllocationUpdateRequestDto {

    private Long id;
    private String seqType;
    private String deviceId;
    private Long fromNo;
    private Long toNo;
    private Long nextLocalNo;
    private Integer allocationSize;
    private String status;
}
