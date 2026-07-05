package za.co.mawa.bes.dto.v2.funeral;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class FuneralServiceRequestResponseDto {
    private String id;
    private String serviceRequestNo;
    private String mortuaryInventoryId;
    private String deceasedName;
    private String deceasedIdentityNumber;
    private String deceasedPartnerId;
    private String packageId;
    private String familyRepId;
    private LocalDate funeralDate;
    private String funeralArea;
    private String deathCertificateNo;
    private String causeOfDeath;
    private Long totalAmountCents;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
