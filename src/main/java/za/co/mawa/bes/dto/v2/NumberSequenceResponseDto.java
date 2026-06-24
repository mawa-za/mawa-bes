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
public class NumberSequenceResponseDto {

    private Long id;
    private String seqType;
    private Long nextNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
