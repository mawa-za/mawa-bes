package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class FuneralServiceRequestDto {
    private String mortuaryInventoryId;
    private String deceasedName;
    private String deceasedIdentityNumber;
    private String deceasedPartnerId;
    private String packageId;
    private String familyRepId;
    private LocalDate funeralDate;
    private String funeralArea;
    private List<FuneralExtraDto> extras;
}
