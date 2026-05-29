package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseInvoiceGenerateRequest {
    /**
     * Pass the invoice ID returned by your existing invoice module.
     * If null, this service returns invoice-ready lines only and does not mark entries as billed.
     */
    private String invoiceId;

    /**
     * Set true only after the invoice has been successfully created.
     */
    private Boolean markAsBilled;

    private String createdBy;
}
