package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CaseDisbursementType;

import java.time.LocalDate;

@Getter
@Setter
public class CaseDisbursementCreateRequest {
    private LocalDate disbursementDate;
    private CaseDisbursementType disbursementType;
    private String description;
    private Long amountCents;
    private Boolean billable;
}
