package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import za.co.mawa.bes.enums.CaseDisbursementType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDisbursementUpdateRequestDto {

    private String id;
    private String caseId;
    private LocalDate disbursementDate;
    private CaseDisbursementType disbursementType;
    private String description;
    private Long amountCents;
    private Boolean billable;
    private Boolean billed;
    private String invoiceId;
    private Boolean paidFromTrust;
    private String trustTransactionId;
}
