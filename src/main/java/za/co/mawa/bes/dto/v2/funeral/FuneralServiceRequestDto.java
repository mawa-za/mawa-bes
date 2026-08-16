package za.co.mawa.bes.dto.v2.funeral;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class FuneralServiceRequestDto {
    private String mortuaryInventoryId;
    private String serviceRequestNo;
    private String deceasedName;
    private String deceasedIdentityNumber;
    private String deceasedPartnerId;
    private String packageId;
    @JsonAlias({"familyRepPartnerId", "familyRepresentativePartnerId"})
    private String familyRepId;
    @JsonAlias({"familyRepNames", "familyRepresentativeFirstNames", "familyRepresentativeNames"})
    private String familyRepresentativeNames;
    @JsonAlias({"familyRepSurname", "familyRepresentativeLastName", "familyRepresentativeSurname"})
    private String familyRepresentativeSurname;
    @JsonAlias({"familyRepContactDetails", "familyRepresentativeContact", "familyRepresentativeContactDetails"})
    private String familyRepresentativeContactDetails;
    @JsonAlias({"deathDate", "dateOfDeath"})
    private LocalDate dateOfDeath;
    private LocalDate funeralDate;
    @JsonAlias({"funeralLocation", "serviceLocation"})
    private String funeralArea;
    @JsonAlias({"deliveryDirections", "directionsToDeliveryLocation", "deceasedDeliveryLocationDirections"})
    private String deceasedDeliveryDirections;
    @JsonAlias({"deliveryDateTime", "deceasedDeliveryDateTime"})
    private LocalDateTime deceasedDeliveryDateTime;
    @JsonAlias({"certificateNumber", "deathCertificateNumber", "deathCertificateNo"})
    private String deathCertificateNo;
    private String causeOfDeath;
    private List<FuneralExtraDto> extras;
}
