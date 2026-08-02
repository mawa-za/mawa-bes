package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumberSequenceResponseDto {
    private Long id;
    private String seqType;
    private String description;
    private String prefix;
    private String separator;
    private Integer paddingLength;
    private String nextFormattedNumber;
    private Long startNo;
    private Long nextNo;
    private Long endNo;
    private Long remainingNumbers;
    private Integer defaultAllocationSize;
    private Long warningThreshold;
    private Boolean active;
    private Boolean exhausted;
    private Boolean lowRange;
    private Long lockVersion;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
