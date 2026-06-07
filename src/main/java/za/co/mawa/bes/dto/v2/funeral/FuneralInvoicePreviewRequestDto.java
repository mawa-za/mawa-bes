package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FuneralInvoicePreviewRequestDto {
    /** Preferred once the arrangement exists. */
    private String funeralServiceId;

    /** Backwards-compatible preview fields. */
    private String deceasedName;
    private String packageId;
    private String familyRepId;
    private List<String> memberships;
    private List<FuneralExtraDto> extras;
}
