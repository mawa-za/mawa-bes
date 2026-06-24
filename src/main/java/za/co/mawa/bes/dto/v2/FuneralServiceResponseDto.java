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
public class FuneralServiceResponseDto {

    private String id;
    private String mortuaryInventoryId;
    private String deceasedPartnerId;
    private String deceasedName;
    private String deceasedIdentityNumber;
    private String packageId;
    private String familyRepId;
    private LocalDate funeralDate;
    private String funeralArea;
    private Long totalAmountCents;
    private String extrasJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
