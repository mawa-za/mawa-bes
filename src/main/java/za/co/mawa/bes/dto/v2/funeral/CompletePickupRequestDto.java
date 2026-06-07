package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CompletePickupRequestDto {
    private LocalDateTime completionTime;
}
