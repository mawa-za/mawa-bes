package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumberSequenceCreateRequestDto {
    private String seqType;
    private String description;
    private String prefix;
    private String separator;
    private Integer paddingLength;
    private Long startNo;
    private Long nextNo;
    private Long endNo;
    private Integer defaultAllocationSize;
    private Long warningThreshold;
    private Boolean active;
}
