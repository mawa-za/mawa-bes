package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MortuaryCheckoutDto {
    private String releaseTo;
    private String identityNumber;
    private LocalDateTime checkoutDate;
}
