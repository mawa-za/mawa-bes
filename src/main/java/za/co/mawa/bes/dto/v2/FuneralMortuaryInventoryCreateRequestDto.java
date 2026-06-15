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
public class FuneralMortuaryInventoryCreateRequestDto {

    private String pickupRequestId;
    private String deceasedPartnerId;
    private String deceasedName;
    private String tagNumber;
    private LocalDateTime checkInDate;
    private String status;
    private String releaseTo;
    private String identityNumber;
    private LocalDateTime checkoutDate;
}
