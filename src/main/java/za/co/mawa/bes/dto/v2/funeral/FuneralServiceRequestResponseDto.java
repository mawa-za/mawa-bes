package za.co.mawa.bes.dto.v2.funeral;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class FuneralServiceRequestResponseDto {
    private String id;
    private String mortuaryInventoryId;
    private String deceasedName;
    private String deceasedIdentityNumber;
    private String deceasedPartnerId;
    private String packageId;
    private String familyRepId;
    private LocalDate funeralDate;
    private String funeralArea;
    private Long totalAmountCents;
    private String status;
}
