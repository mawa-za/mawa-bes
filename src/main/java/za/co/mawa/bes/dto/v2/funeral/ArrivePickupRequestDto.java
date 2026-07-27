package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ArrivePickupRequestDto {
    private LocalDateTime arrivalTime;
    private Boolean corpseInjured;
    private String injuryDetails;
}
