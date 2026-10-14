package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class ManualReceiptCutoverConfigurationDto {
    private LocalDate mawaPayGoLiveDate;
    private LocalDate legacyCaptureCloseDate;
    private Boolean emergencyReceiptRequiresProof;
    private Boolean legacyCaptureEnabled;
    private String updatedBy;
}
