package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CaseInvoiceGenerationResponse {
    private String caseId;
    private String caseNo;
    private String title;
    private String invoiceId;
    private Long lineCount;
    private Long totalAmountCents;
    private Boolean markedAsBilled;
    private List<CaseInvoiceLineResponse> lines;
}
