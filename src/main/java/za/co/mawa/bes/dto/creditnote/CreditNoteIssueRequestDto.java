package za.co.mawa.bes.dto.creditnote;

import lombok.Data;

@Data
public class CreditNoteIssueRequestDto {
    private Long amountCents;
    private String reason;
}
