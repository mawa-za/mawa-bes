package za.co.mawa.bes.dto.v2.funeral;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FuneralInvoicePreviewRequestDto {
    /** Preferred once the arrangement exists. Accept legacy Flutter payload keys too. */
    @JsonAlias({"serviceRequestId", "serviceId"})
    private String funeralServiceId;

    /** Backwards-compatible preview fields. */
    private String deceasedName;
    private String packageId;
    private String familyRepId;
    private List<String> memberships;
    private List<FuneralExtraDto> extras;
    private String claimType;

    public String getEffectiveClaimType(int selectedCoverCount) {
        String value = claimType == null ? "" : claimType.trim().toUpperCase();
        if ("COMBINATION".equals(value)) return "COMBINATION";
        if ("FUNERAL".equals(value)) return "FUNERAL";
        return selectedCoverCount > 1 ? "COMBINATION" : "FUNERAL";
    }
}
