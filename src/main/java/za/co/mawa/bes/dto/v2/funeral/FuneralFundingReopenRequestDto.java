package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FuneralFundingReopenRequestDto {
    /** Mandatory audit reason for reversing the issued funeral invoices. */
    private String reason;
}
